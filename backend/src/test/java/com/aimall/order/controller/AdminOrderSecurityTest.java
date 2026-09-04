package com.aimall.order.controller;

import com.aimall.auth.mapper.MallUserMapper;
import com.aimall.auth.service.JwtService;
import com.aimall.config.SecurityConfig;
import com.aimall.order.service.AdminOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Admin 订单安全测试：匿名请求必须在 Controller 前被 Spring Security 拦截。 */
@WebMvcTest(controllers = AdminOrderController.class)
@Import(SecurityConfig.class)
class AdminOrderSecurityTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdminOrderService adminOrderService;

    /** JWT 过滤器依赖的令牌服务使用 Mock，匿名请求不会真正解析 Bearer Token。 */
    @MockitoBean
    private JwtService jwtService;

    /** JWT 过滤器依赖的用户 Mapper 使用 Mock，匿名请求不会查询数据库。 */
    @MockitoBean
    private MallUserMapper mallUserMapper;

    @Test
    /** 未登录不能读取后台订单，避免客户订单数据被公开访问。 */
    void anonymousCannotReadAdminOrders() throws Exception {
        mvc.perform(get("/api/v1/admin/orders"))
                .andExpect(status().isUnauthorized());
    }
}
