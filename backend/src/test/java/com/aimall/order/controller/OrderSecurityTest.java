package com.aimall.order.controller;

import com.aimall.auth.mapper.MallUserMapper;
import com.aimall.auth.service.JwtService;
import com.aimall.config.SecurityConfig;
import com.aimall.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 订单安全边界测试。
 *
 * <p>Controller 单元测试可以手动注入 Authentication，但这里额外启动真实的
 * Spring Security 过滤链，确认没有 Bearer Token 的游客不能访问“我的订单”。</p>
 */
@WebMvcTest(controllers = OrderController.class)
@Import(SecurityConfig.class)
class OrderSecurityTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OrderService orderService;

    /** JWT 过滤器依赖的令牌服务由 Mock 提供，匿名请求不会真正解析令牌。 */
    @MockitoBean
    private JwtService jwtService;

    /** JWT 过滤器依赖的用户 Mapper 由 Mock 提供，匿名请求不会访问数据库。 */
    @MockitoBean
    private MallUserMapper mallUserMapper;

    @Test
    /** 订单接口没有显式放入公开白名单，因此匿名访问必须由过滤链返回 401。 */
    void anonymousCannotReadMyOrders() throws Exception {
        mvc.perform(get("/api/v1/me/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    /** 取消订单也属于用户私有接口，匿名请求不能进入订单 Service。 */
    void anonymousCannotCancelMyOrder() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/me/orders/1/cancellation"))
                .andExpect(status().isUnauthorized());
    }
}
