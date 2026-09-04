package com.aimall.order.controller;

import com.aimall.common.exception.GlobalExceptionHandler;
import com.aimall.order.exception.OrderNotFoundException;
import com.aimall.order.exception.OrderRuleException;
import com.aimall.order.service.AdminOrderService;
import com.aimall.order.vo.AdminOrderDetailResponse;
import com.aimall.order.vo.AdminOrderPageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Admin 订单 Controller 契约测试：验证分页参数、详情 JSON 和业务错误码。 */
class AdminOrderControllerTest {
    private final AdminOrderService service = mock(AdminOrderService.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new AdminOrderController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    /** 后台列表把状态、订单号和客户 ID 一起交给 Service，响应使用统一分页字段。 */
    void listPassesFiltersAndReturnsPage() throws Exception {
        when(service.list(2, 10, "PENDING_PICKUP", "AM2026", 7L))
                .thenReturn(new AdminOrderPageResponse(List.of(), 2, 10, 0, 0));

        mvc.perform(get("/api/v1/admin/orders")
                        .param("page", "2")
                        .param("pageSize", "10")
                        .param("status", "PENDING_PICKUP")
                        .param("orderNo", "AM2026")
                        .param("userId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.items").isArray());

        verify(service).list(2, 10, "PENDING_PICKUP", "AM2026", 7L);
    }

    @Test
    /** 订单详情返回客户展示信息和订单状态，但没有取货码字段。 */
    void getReturnsAdminDetail() throws Exception {
        AdminOrderDetailResponse detail = new AdminOrderDetailResponse(
                "123", "AM202609040001", "7", "customer-7", "小明",
                "PENDING_PICKUP", "默认取货点", "取货地址", 2,
                new BigDecimal("24.60"), List.of(), OffsetDateTime.now(), null, null);
        when(service.get(123L)).thenReturn(detail);

        mvc.perform(get("/api/v1/admin/orders/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("123"))
                .andExpect(jsonPath("$.displayName").value("小明"))
                .andExpect(jsonPath("$.status").value("PENDING_PICKUP"))
                .andExpect(jsonPath("$.pickupCodeHash").doesNotExist());

        verify(service).get(123L);
    }

    @Test
    /** 状态筛选传入未知值时返回 400，Service 只需要维护业务校验。 */
    void listMapsInvalidStatusToBadRequest() throws Exception {
        when(service.list(1, 20, "PAYING", null, null))
                .thenThrow(new OrderRuleException("订单状态筛选不合法"));

        mvc.perform(get("/api/v1/admin/orders").param("status", "PAYING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ORDER_RULE_INVALID"));
    }

    @Test
    /** Service 找不到订单时，Controller 通过全局异常处理器返回稳定的 404。 */
    void getMapsMissingOrderToNotFound() throws Exception {
        when(service.get(404L)).thenThrow(new OrderNotFoundException(404L));

        mvc.perform(get("/api/v1/admin/orders/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));

        verify(service).get(404L);
    }
}
