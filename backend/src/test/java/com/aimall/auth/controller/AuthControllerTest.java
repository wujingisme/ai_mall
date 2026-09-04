package com.aimall.auth.controller;

import com.aimall.auth.dto.LoginRequest;
import com.aimall.auth.exception.AccountLockedException;
import com.aimall.auth.exception.InvalidCredentialsException;
import com.aimall.auth.exception.UsernameAlreadyExistsException;
import com.aimall.auth.exception.WechatLoginException;
import com.aimall.auth.dto.RefreshTokenRequest;
import com.aimall.auth.service.AuthService;
import com.aimall.auth.vo.CurrentUserResponse;
import com.aimall.auth.vo.TokenResponse;
import com.aimall.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/** Controller 层认证契约测试：验证 URL、参数校验、状态码和统一错误码。 */
class AuthControllerTest {
    private final AuthService service = mock(AuthService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    /** 合法注册返回 201，并确认 Service 返回的角色是 CUSTOMER。 */
    void registerCreatesCustomer() throws Exception {
        var user = new CurrentUserResponse("2", "new_user", "新用户", null, List.of("CUSTOMER"));
        when(service.register(any())).thenReturn(user);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new_user\",\"password\":\"User123!\",\"displayName\":\"新用户\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new_user"))
                .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"));
    }

    @Test
    /** 非法用户名在进入 Service 前被 DTO 校验拦截。 */
    void registerRejectsInvalidUsername() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bad name\",\"password\":\"User123!\",\"displayName\":\"用户\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    /** Service 抛出用户名冲突时，Controller 层应返回 409。 */
    void duplicateUsernameIsConflict() throws Exception {
        when(service.register(any())).thenThrow(new UsernameAlreadyExistsException());
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"existing\",\"password\":\"User123!\",\"displayName\":\"用户\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_ALREADY_EXISTS"));
    }

    @Test
    /** 登录成功响应保留 Bearer 类型、访问令牌和用户公开资料。 */
    void loginReturnsContractResponse() throws Exception {
        var user = new CurrentUserResponse("1", "admin", "商城管理员", null, List.of("ADMIN"));
        when(service.login(any())).thenReturn(new TokenResponse("Bearer", "jwt", 900,
                "a-refresh-token-with-more-than-32-characters", 604800, user));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.id").value("1"));
    }

    @Test
    /** 空请求体只产生 400，业务 Service 不应被调用。 */
    void loginRejectsInvalidBody() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    @Test
    /** 微信登录把一次性 code 原样交给 Service，响应结构与普通登录一致。 */
    void wechatLoginReturnsContractResponse() throws Exception {
        var user = new CurrentUserResponse("2", "wx_open-id", "微信用户", null, List.of("CUSTOMER"));
        when(service.loginWithWechat("one-time-code")).thenReturn(new TokenResponse("Bearer", "jwt", 900,
                "a-refresh-token-with-more-than-32-characters", 604800, user));

        mvc.perform(post("/api/v1/auth/wechat/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"one-time-code\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.id").value("2"))
                .andExpect(jsonPath("$.user.roles[0]").value("CUSTOMER"));
        verify(service).loginWithWechat("one-time-code");
    }

    @Test
    /** 空白微信 code 应由请求校验拦截，而不是访问微信上游。 */
    void wechatLoginRejectsBlankCode() throws Exception {
        mvc.perform(post("/api/v1/auth/wechat/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    @Test
    /** 微信无效凭证映射为专用错误码和 401。 */
    void wechatLoginFailureUsesStableErrorCode() throws Exception {
        when(service.loginWithWechat("expired-code")).thenThrow(new WechatLoginException(
                WechatLoginException.Failure.INVALID_CREDENTIAL, "微信登录凭证无效或已过期"));
        mvc.perform(post("/api/v1/auth/wechat/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"expired-code\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("WECHAT_LOGIN_FAILED"));
    }

    @Test
    /** 微信服务不可用映射为 503，不能误报成用户凭证错误。 */
    void wechatServiceFailureIsNotReportedAsInvalidUserCredential() throws Exception {
        when(service.loginWithWechat("valid-code")).thenThrow(new WechatLoginException(
                WechatLoginException.Failure.SERVICE_UNAVAILABLE, "暂时无法连接微信登录服务"));
        mvc.perform(post("/api/v1/auth/wechat/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"valid-code\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("WECHAT_SERVICE_UNAVAILABLE"));
    }

    @Test
    /** 密码错误统一返回 401。 */
    void wrongPasswordIsUnauthorized() throws Exception {
        when(service.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Wrong123!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    /** 账号锁定返回 423，并携带前端可使用的 Retry-After。 */
    void lockedAccountReturnsRetryAfter() throws Exception {
        when(service.login(any(LoginRequest.class))).thenThrow(new AccountLockedException(900));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Wrong123!\"}"))
                .andExpect(status().isLocked()).andExpect(header().string("Retry-After", "900"));
    }

    @Test
    /** 刷新接口返回新访问令牌，轮换细节由 Service 测试负责。 */
    void refreshRotatesTokens() throws Exception {
        var user = new CurrentUserResponse("1", "admin", "商城管理员", null, List.of("ADMIN"));
        when(service.refresh(any(RefreshTokenRequest.class))).thenReturn(new TokenResponse("Bearer", "new-jwt", 900,
                "a-new-refresh-token-with-more-than-32-characters", 604800, user));
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"an-old-refresh-token-with-more-than-32-characters\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").value("new-jwt"));
    }

    @Test
    /** 退出登录成功返回 204，并确认请求确实委托给 Service。 */
    void logoutIsNoContent() throws Exception {
        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"a-refresh-token-with-more-than-32-characters\"}"))
                .andExpect(status().isNoContent());
        verify(service).logout(any(RefreshTokenRequest.class));
    }

    @Test
    /** Authentication 中的 principal 被作为当前用户 ID 传给 Service。 */
    void meReturnsAuthenticatedUser() throws Exception {
        var user = new CurrentUserResponse("1", "admin", "商城管理员", null, List.of("ADMIN"));
        when(service.currentUser("1")).thenReturn(user);
        mvc.perform(get("/api/v1/auth/me")
                        .principal(new UsernamePasswordAuthenticationToken("1", null, List.of())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("admin"));
    }
}
