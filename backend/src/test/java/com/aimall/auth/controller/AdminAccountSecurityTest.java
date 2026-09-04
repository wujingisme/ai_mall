package com.aimall.auth.controller;

import com.aimall.auth.mapper.MallUserMapper;
import com.aimall.auth.service.AuthService;
import com.aimall.auth.service.JwtService;
import com.aimall.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 后台账号安全测试：匿名请求不能自助修改自己的角色。 */
@WebMvcTest(controllers = AdminAccountController.class)
@Import(SecurityConfig.class)
class AdminAccountSecurityTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService authService;

    /** JWT 过滤器依赖的令牌服务使用 Mock，匿名请求不会真正解析令牌。 */
    @MockitoBean
    private JwtService jwtService;

    /** JWT 过滤器依赖的用户 Mapper 使用 Mock，匿名请求不会查询数据库。 */
    @MockitoBean
    private MallUserMapper mallUserMapper;

    @Test
    /** 没有后台 JWT 时，追加 CUSTOMER 角色必须在 Controller 前返回 401。 */
    void anonymousCannotEnableCustomerRole() throws Exception {
        mvc.perform(post("/api/v1/admin/accounts/me/customer-role"))
                .andExpect(status().isUnauthorized());
    }
}
