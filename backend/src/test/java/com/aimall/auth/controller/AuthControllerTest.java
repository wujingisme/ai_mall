package com.aimall.auth.controller;

import com.aimall.auth.dto.LoginRequest;
import com.aimall.auth.exception.AccountLockedException;
import com.aimall.auth.exception.InvalidCredentialsException;
import com.aimall.auth.exception.UsernameAlreadyExistsException;
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

class AuthControllerTest {
    private final AuthService service = mock(AuthService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
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
    void registerRejectsInvalidUsername() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bad name\",\"password\":\"User123!\",\"displayName\":\"用户\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void duplicateUsernameIsConflict() throws Exception {
        when(service.register(any())).thenThrow(new UsernameAlreadyExistsException());
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"existing\",\"password\":\"User123!\",\"displayName\":\"用户\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_ALREADY_EXISTS"));
    }

    @Test
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
    void loginRejectsInvalidBody() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        when(service.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Wrong123!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void lockedAccountReturnsRetryAfter() throws Exception {
        when(service.login(any(LoginRequest.class))).thenThrow(new AccountLockedException(900));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Wrong123!\"}"))
                .andExpect(status().isLocked()).andExpect(header().string("Retry-After", "900"));
    }

    @Test
    void refreshRotatesTokens() throws Exception {
        var user = new CurrentUserResponse("1", "admin", "商城管理员", null, List.of("ADMIN"));
        when(service.refresh(any(RefreshTokenRequest.class))).thenReturn(new TokenResponse("Bearer", "new-jwt", 900,
                "a-new-refresh-token-with-more-than-32-characters", 604800, user));
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"an-old-refresh-token-with-more-than-32-characters\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").value("new-jwt"));
    }

    @Test
    void logoutIsNoContent() throws Exception {
        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"a-refresh-token-with-more-than-32-characters\"}"))
                .andExpect(status().isNoContent());
        verify(service).logout(any(RefreshTokenRequest.class));
    }

    @Test
    void meReturnsAuthenticatedUser() throws Exception {
        var user = new CurrentUserResponse("1", "admin", "商城管理员", null, List.of("ADMIN"));
        when(service.currentUser("1")).thenReturn(user);
        mvc.perform(get("/api/v1/auth/me")
                        .principal(new UsernamePasswordAuthenticationToken("1", null, List.of())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("admin"));
    }
}
