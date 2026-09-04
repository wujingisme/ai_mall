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
/**
 * 认证业务服务。
 *
 * <p>这里把“注册/登录/微信登录/刷新/退出”这些跨越数据库和安全组件的流程串起来。
 * Controller 只负责接 HTTP；本类负责决定业务是否允许继续，以及何时写入用户和会话表。</p>
 */
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
    /**
     * 微信小程序登录流程：一次性 code -> 微信 OpenID -> 本地 CUSTOMER 用户 -> 商城令牌。
     *
     * <p>同一个 OpenID 再次登录会复用原用户；首次插入遇到唯一键冲突时重新查询，
     * 这是为了处理两个并发请求同时第一次登录同一个微信用户的情况。</p>
     */
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
    /** 消费者注册固定写入 CUSTOMER 角色，防止客户端通过注册接口获得后台权限。 */
    public CurrentUserResponse register(RegisterRequest request) {
        // 消费者自助注册只能获得 CUSTOMER，绝不能获得可访问管理后台的角色。
        return createUser(request.username(), request.password(), request.displayName(), "CUSTOMER");
    }

    @Transactional
    /** 由已授权的超级管理员创建日常后台账号。 */
    public CurrentUserResponse createAdminAccount(AdminAccountCreateRequest request) {
        return createUser(request.username(), request.password(), request.displayName(), request.role());
    }

    private CurrentUserResponse createUser(String username, String password, String displayName, String role) {
        // 先查一次可以给出友好的业务错误；真正的并发安全仍依赖数据库 username 唯一索引。
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
    /**
     * 密码登录。
     *
     * <p>失败次数的更新必须提交，所以认证失败异常不回滚事务；否则用户永远不会被锁定。
     * 用户不存在和密码错误统一返回，避免通过响应差异枚举有效用户名。</p>
     */
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
    /**
     * 刷新令牌轮换：哈希查询旧会话，条件撤销旧会话，再创建新会话。
     * 条件撤销的返回行数必须是 1，才能证明当前请求赢得了并发竞争。
     */
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
    /** 撤销刷新令牌；找不到令牌也不报错，使网络重试不会让退出按钮变红。 */
    public void logout(RefreshTokenRequest request) {
        LocalDateTime now = LocalDateTime.now();
        // 退出必须幂等：令牌不存在或已撤销时也直接视为成功。
        sessionMapper.update(null, new LambdaUpdateWrapper<AuthSession>()
                .eq(AuthSession::getRefreshTokenHash, sha256(request.refreshToken()))
                .isNull(AuthSession::getRevokedAt)
                .set(AuthSession::getRevokedAt, now));
        log.info("退出登录完成");
    }

    /** 根据过滤器放入的用户 ID 查询当前用户，并再次确认账号仍处于启用状态。 */
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
        // 数据库只保存刷新令牌的 SHA-256 摘要；明文只在这次响应中返回给客户端。
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
        // 角色在数据库中以逗号分隔保存，返回前拆成 JSON 数组并去空、去重。
        List<String> roles = Arrays.stream(user.getRoles().split(",")).map(String::trim)
                .filter(role -> !role.isEmpty()).distinct().toList();
        return new CurrentUserResponse(user.getId().toString(), user.getUsername(),
                user.getDisplayName(), user.getAvatarUrl(), roles);
    }

    private void recordFailure(MallUser user, LocalDateTime now) {
        // Optional 兼容历史数据中可能为 null 的失败次数；达到阈值后记录锁定截止时间。
        int failures = Optional.ofNullable(user.getFailedLoginAttempts()).orElse(0) + 1;
        user.setFailedLoginAttempts(failures);
        if (failures >= maxFailures) user.setLockedUntil(now.plusSeconds(lockSeconds));
        userMapper.updateById(user);
        if (failures >= maxFailures) throw new AccountLockedException(lockSeconds);
    }

    private String randomToken() {
        // SecureRandom 生成不可预测字节，Base64 URL-safe 形式便于放进 JSON 和 HTTP 请求。
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        // 数据库泄露时不能直接拿摘要当刷新令牌使用；前端永远拿不到这里的 hash。
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("无法生成刷新令牌摘要", e);
        }
    }
}
