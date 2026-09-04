package com.aimall.order.controller;

import com.aimall.common.exception.GlobalExceptionHandler;
import com.aimall.order.dto.OrderCreateRequest;
import com.aimall.order.dto.OrderPreviewRequest;
import com.aimall.order.service.OrderService;
import com.aimall.order.vo.OrderCreateResponse;
import com.aimall.order.vo.OrderDetailResponse;
import com.aimall.order.vo.OrderPreviewResponse;
import com.aimall.order.vo.OrderPageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 订单 Controller HTTP 契约测试。
 *
 * <p>standalone MockMvc 不启动完整 SecurityFilterChain，所以测试手动放入
 * Authentication；重点验证请求体校验、JWT 用户 ID 传递和响应 JSON 结构。</p>
 */
class OrderControllerTest {
    private final OrderService service = mock(OrderService.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new OrderController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    /** 合法预览请求返回计算结果，并把 Authentication 中的用户 ID 传给 Service。 */
    void previewUsesAuthenticatedUserAndReturnsJson() throws Exception {
        when(service.preview(eq(7L), any(OrderPreviewRequest.class)))
                .thenReturn(new OrderPreviewResponse(List.of(), "取货点", "取货地址", 0, new BigDecimal("0.00")));

        mvc.perform(post("/api/v1/orders/preview")
                        .principal(new UsernamePasswordAuthenticationToken("7", null, List.of()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":\"3\",\"quantity\":2}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pickupLocationName").value("取货点"))
                .andExpect(jsonPath("$.totalQuantity").value(0));

        verify(service).preview(eq(7L), any(OrderPreviewRequest.class));
    }

    @Test
    /** 空请求体由 DTO 校验拦截并返回 400，避免 Service 收到不完整数据。 */
    void previewRejectsEmptyItems() throws Exception {
        mvc.perform(post("/api/v1/orders/preview")
                        .principal(new UsernamePasswordAuthenticationToken("7", null, List.of()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(service);
    }

    @Test
    /** 正式下单响应同时返回订单快照、首次展示的取货码和 replayed 标记。 */
    void createUsesAuthenticatedUserAndReturnsPickupCode() throws Exception {
        OrderDetailResponse detail = new OrderDetailResponse(
                "123", "AM202609040001", "PENDING_PICKUP", "取货点", "取货地址",
                2, new BigDecimal("24.60"), List.of(), OffsetDateTime.now(), null, null);
        when(service.create(eq(7L), any(OrderCreateRequest.class)))
                .thenReturn(new OrderCreateResponse(detail, "ABCD2345", false));

        mvc.perform(post("/api/v1/orders")
                        .principal(new UsernamePasswordAuthenticationToken("7", null, List.of()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":\"3\",\"quantity\":2}],\"clientRequestId\":\"client-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.id").value("123"))
                .andExpect(jsonPath("$.pickupCode").value("ABCD2345"))
                .andExpect(jsonPath("$.replayed").value(false));

        verify(service).create(eq(7L), any(OrderCreateRequest.class));
    }

    @Test
    /** 缺少幂等键时在 Controller 层直接返回 400，不让 Service 进入库存事务。 */
    void createRejectsMissingIdempotencyKey() throws Exception {
        mvc.perform(post("/api/v1/orders")
                        .principal(new UsernamePasswordAuthenticationToken("7", null, List.of()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":\"3\",\"quantity\":2}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(service);
    }

    @Test
    /** “我的订单”列表只接收查询参数，并把分页和状态原样交给 Service。 */
    void listPassesPagingAndStatus() throws Exception {
        when(service.list(7L, 2, 10, "PENDING_PICKUP"))
                .thenReturn(new OrderPageResponse(List.of(), 2, 10, 0, 0));

        mvc.perform(get("/api/v1/me/orders")
                        .principal(new UsernamePasswordAuthenticationToken("7", null, List.of()))
                        .param("page", "2")
                        .param("pageSize", "10")
                        .param("status", "PENDING_PICKUP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.pageSize").value(10));

        verify(service).list(7L, 2, 10, "PENDING_PICKUP");
    }
}
