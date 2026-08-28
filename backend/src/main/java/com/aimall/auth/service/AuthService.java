package com.aimall.auth.service;

import com.aimall.auth.dto.LoginRequest;
import com.aimall.auth.dto.RegisterRequest;
import com.aimall.auth.entity.AuthSession;
import com.aimall.auth.entity.MallUser;
import com.aimall.auth.exception.*;
import com.aimall.auth.mapper.AuthSessionMapper;
import com.aimall.auth.mapper.MallUserMapper;
import com.aimall.auth.vo.CurrentUserResponse;
import com.aimall.auth.vo.TokenResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
    private final SecureRandom secureRandom = new SecureRandom();
    private final long accessTokenSeconds;
    private final long refreshTokenSeconds;
    private final int maxFailures;
    private final long lockSeconds;

    public AuthService(MallUserMapper userMapper, AuthSessionMapper sessionMapper,
                       PasswordEncoder passwordEncoder, JwtService jwtService,
                       @Value("${auth.access-token-seconds}") long accessTokenSeconds,
                       @Value("${auth.refresh-token-seconds}") long refreshTokenSeconds,
                       @Value("${auth.max-login-failures}") int maxFailures,
                       @Value("${auth.lock-seconds}") long lockSeconds) {
        this.userMapper = userMapper;
        this.sessionMapper = sessionMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.accessTokenSeconds = accessTokenSeconds;
        this.refreshTokenSeconds = refreshTokenSeconds;
        this.maxFailures = maxFailures;
        this.lockSeconds = lockSeconds;
    }

    @Transactional
    public CurrentUserResponse register(RegisterRequest request) {
        // 自助注册只授予最低业务角色，禁止客户端借注册接口提升权限。
        boolean exists = userMapper.selectCount(new LambdaQueryWrapper<MallUser>()
                .eq(MallUser::getUsername, request.username())) > 0;
        if (exists) throw new UsernameAlreadyExistsException();

        MallUser user = new MallUser();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setRoles("OPERATOR");
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
                user.getDisplayName(), null, List.of("OPERATOR"));
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

        String refreshToken = randomToken();
        AuthSession session = new AuthSession();
        String sessionId = UUID.randomUUID().toString();
        session.setId(sessionId);
        session.setUserId(user.getId());
        session.setRefreshTokenHash(sha256(refreshToken));
        session.setExpiresAt(now.plusSeconds(refreshTokenSeconds));
        session.setCreatedAt(now);
        sessionMapper.insert(session);
        log.info("登录成功: userId={}, sessionId={}", user.getId(), sessionId);

        List<String> roles = Arrays.stream(user.getRoles().split(",")).map(String::trim)
                .filter(role -> !role.isEmpty()).distinct().toList();
        CurrentUserResponse currentUser = new CurrentUserResponse(user.getId().toString(), user.getUsername(),
                user.getDisplayName(), user.getAvatarUrl(), roles);
        return new TokenResponse("Bearer", jwtService.createAccessToken(user), accessTokenSeconds,
                refreshToken, refreshTokenSeconds, currentUser);
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
