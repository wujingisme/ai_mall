package com.aimall.auth.service;

import com.aimall.auth.dto.LoginRequest;
import com.aimall.auth.dto.RegisterRequest;
import com.aimall.auth.dto.AdminAccountCreateRequest;
import com.aimall.auth.dto.RefreshTokenRequest;
import com.aimall.auth.entity.AuthSession;
import com.aimall.auth.entity.MallUser;
import com.aimall.auth.exception.*;
import com.aimall.auth.mapper.AuthSessionMapper;
import com.aimall.auth.mapper.MallUserMapper;
import com.aimall.auth.vo.CurrentUserResponse;
import com.aimall.auth.vo.TokenResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final MallUserMapper userMapper;
    private final AuthSessionMapper sessionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final WechatMiniProgramClient wechatClient;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long accessTokenSeconds;
    private final long refreshTokenSeconds;
    private final int maxFailures;
    private final long lockSeconds;

    public AuthService(MallUserMapper userMapper, AuthSessionMapper sessionMapper,
                       PasswordEncoder passwordEncoder, JwtService jwtService, WechatMiniProgramClient wechatClient,
                       @Value("${auth.access-token-seconds}") long accessTokenSeconds,
                       @Value("${auth.refresh-token-seconds}") long refreshTokenSeconds,
                       @Value("${auth.max-login-failures}") int maxFailures,
                       @Value("${auth.lock-seconds}") long lockSeconds) {
        this.userMapper = userMapper;
        this.sessionMapper = sessionMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.wechatClient = wechatClient;
        this.accessTokenSeconds = accessTokenSeconds;
        this.refreshTokenSeconds = refreshTokenSeconds;
        this.maxFailures = maxFailures;
        this.lockSeconds = lockSeconds;
    }

    @Transactional
    public TokenResponse loginWithWechat(String code) {
        WechatMiniProgramClient.WechatIdentity identity = wechatClient.exchangeCode(code);
        MallUser user = userMapper.selectOne(new LambdaQueryWrapper<MallUser>()
                .eq(MallUser::getWechatOpenId, identity.openId()));
        if (user == null) {
            user = new MallUser();
            user.setUsername("wx_" + identity.openId());
            // 微信用户不使用密码登录，但保留不可猜测的 BCrypt 值以兼容统一用户表约束。
            user.setPasswordHash(passwordEncoder.encode(randomToken()));
            user.setDisplayName("微信用户");
            user.setWechatOpenId(identity.openId());
            user.setWechatUnionId(identity.unionId());
            user.setRoles("CUSTOMER");
            user.setEnabled(true);
            user.setFailedLoginAttempts(0);
            try {
                userMapper.insert(user);
            } catch (DuplicateKeyException e) {
                user = userMapper.selectOne(new LambdaQueryWrapper<MallUser>()
                        .eq(MallUser::getWechatOpenId, identity.openId()));
                if (user == null) throw e;
            }
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) throw new AccountDisabledException();
        TokenResponse response = createSession(user, LocalDateTime.now());
        log.info("微信登录成功: userId={}", user.getId());
        return response;
    }

    @Transactional
    public CurrentUserResponse register(RegisterRequest request) {
        // 消费者自助注册只能获得 CUSTOMER，绝不能获得可访问管理后台的角色。
        return createUser(request.username(), request.password(), request.displayName(), "CUSTOMER");
    }

    @Transactional
    public CurrentUserResponse createAdminAccount(AdminAccountCreateRequest request) {
        return createUser(request.username(), request.password(), request.displayName(), request.role());
    }

    private CurrentUserResponse createUser(String username, String password, String displayName, String role) {
        boolean exists = userMapper.selectCount(new LambdaQueryWrapper<MallUser>()
                .eq(MallUser::getUsername, username)) > 0;
        if (exists) throw new UsernameAlreadyExistsException();

        MallUser user = new MallUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(displayName);
        user.setRoles(role);
        user.setEnabled(true);
        user.setFailedLoginAttempts(0);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发注册仍由数据库唯一索引兜底，并返回稳定的业务错误码。
            throw new UsernameAlreadyExistsException();
        }
        log.info("注册成功: userId={}", user.getId());
        return new CurrentUserResponse(user.getId().toString(), user.getUsername(),
                user.getDisplayName(), null, List.of(role));
    }

    // 登录失败仍需提交失败次数与锁定时间，因此认证类异常不能触发事务回滚。
    @Transactional(noRollbackFor = {InvalidCredentialsException.class, AccountLockedException.class})
    public TokenResponse login(LoginRequest request) {
        LocalDateTime now = LocalDateTime.now();
        MallUser user = userMapper.selectOne(new LambdaQueryWrapper<MallUser>()
                .eq(MallUser::getUsername, request.username()));

        if (user != null && user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new AccountLockedException(Duration.between(now, user.getLockedUntil()).toSeconds());
        }

        // 不区分用户名不存在和密码错误，避免攻击者枚举有效账号。
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            if (user != null) recordFailure(user, now);
            throw new InvalidCredentialsException();
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) throw new AccountDisabledException();
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userMapper.updateById(user);

        TokenResponse response = createSession(user, now);
        log.info("登录成功: userId={}", user.getId());
        return response;
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        LocalDateTime now = LocalDateTime.now();
        String tokenHash = sha256(request.refreshToken());
        AuthSession oldSession = sessionMapper.selectOne(new LambdaQueryWrapper<AuthSession>()
                .eq(AuthSession::getRefreshTokenHash, tokenHash));
        if (oldSession == null || oldSession.getRevokedAt() != null || !oldSession.getExpiresAt().isAfter(now)) {
            throw new RefreshTokenInvalidException();
        }

        // 条件更新保证同一个刷新令牌即使被并发调用，也只能有一个请求完成轮换。
        int updated = sessionMapper.update(null, new LambdaUpdateWrapper<AuthSession>()
                .eq(AuthSession::getId, oldSession.getId())
                .isNull(AuthSession::getRevokedAt)
                .set(AuthSession::getRevokedAt, now));
        if (updated != 1) throw new RefreshTokenInvalidException();

        MallUser user = userMapper.selectById(oldSession.getUserId());
        if (user == null) throw new RefreshTokenInvalidException();
        if (!Boolean.TRUE.equals(user.getEnabled())) throw new AccountDisabledException();
        TokenResponse response = createSession(user, now);
        log.info("刷新令牌轮换成功: userId={}, oldSessionId={}", user.getId(), oldSession.getId());
        return response;
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        LocalDateTime now = LocalDateTime.now();
        // 退出必须幂等：令牌不存在或已撤销时也直接视为成功。
        sessionMapper.update(null, new LambdaUpdateWrapper<AuthSession>()
                .eq(AuthSession::getRefreshTokenHash, sha256(request.refreshToken()))
                .isNull(AuthSession::getRevokedAt)
                .set(AuthSession::getRevokedAt, now));
        log.info("退出登录完成");
    }

    public CurrentUserResponse currentUser(String userId) {
        MallUser user;
        try {
            user = userMapper.selectById(Long.valueOf(userId));
        } catch (NumberFormatException e) {
            throw new InvalidCredentialsException();
        }
        if (user == null) throw new InvalidCredentialsException();
        if (!Boolean.TRUE.equals(user.getEnabled())) throw new AccountDisabledException();
        return toCurrentUser(user);
    }

    private TokenResponse createSession(MallUser user, LocalDateTime now) {
        String refreshToken = randomToken();
        AuthSession session = new AuthSession();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(user.getId());
        session.setRefreshTokenHash(sha256(refreshToken));
        session.setExpiresAt(now.plusSeconds(refreshTokenSeconds));
        session.setCreatedAt(now);
        sessionMapper.insert(session);
        return new TokenResponse("Bearer", jwtService.createAccessToken(user), accessTokenSeconds,
                refreshToken, refreshTokenSeconds, toCurrentUser(user));
    }

    private CurrentUserResponse toCurrentUser(MallUser user) {
        List<String> roles = Arrays.stream(user.getRoles().split(",")).map(String::trim)
                .filter(role -> !role.isEmpty()).distinct().toList();
        return new CurrentUserResponse(user.getId().toString(), user.getUsername(),
                user.getDisplayName(), user.getAvatarUrl(), roles);
    }

    private void recordFailure(MallUser user, LocalDateTime now) {
        int failures = Optional.ofNullable(user.getFailedLoginAttempts()).orElse(0) + 1;
        user.setFailedLoginAttempts(failures);
        if (failures >= maxFailures) user.setLockedUntil(now.plusSeconds(lockSeconds));
        userMapper.updateById(user);
        if (failures >= maxFailures) throw new AccountLockedException(lockSeconds);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("无法生成刷新令牌摘要", e);
        }
    }
}
