package com.aimall.auth.controller;

import com.aimall.auth.dto.LoginRequest;
import com.aimall.auth.exception.AccountLockedException;
import com.aimall.auth.exception.InvalidCredentialsException;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {
    private final AuthService service = mock(AuthService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

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
}
