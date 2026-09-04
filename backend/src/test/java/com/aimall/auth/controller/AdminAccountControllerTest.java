package com.aimall.auth.controller;

import com.aimall.auth.service.AuthService;
import com.aimall.auth.vo.CurrentUserResponse;
import com.aimall.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 后台账号 Controller 契约测试：确认只能把 Authentication 中的本人 ID 交给 Service。 */
class AdminAccountControllerTest {
    private final AuthService authService = mock(AuthService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminAccountController(authService))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    /** 当前后台账号开通消费者身份后返回新的角色列表。 */
    void enableCustomerRoleUsesAuthenticatedUserId() throws Exception {
        when(authService.enableCustomerRole(7L))
                .thenReturn(new CurrentUserResponse("7", "jj", "管理员", null, List.of("ADMIN", "CUSTOMER")));

        mvc.perform(post("/api/v1/admin/accounts/me/customer-role")
                        .principal(new UsernamePasswordAuthenticationToken("7", null, List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("7"))
                .andExpect(jsonPath("$.roles[1]").value("CUSTOMER"));

        verify(authService).enableCustomerRole(7L);
    }
}
