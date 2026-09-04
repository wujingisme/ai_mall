package com.aimall.product.controller;

import com.aimall.common.exception.GlobalExceptionHandler;
import com.aimall.common.exception.ProductStockConflictException;
import com.aimall.product.dto.ProductWriteRequest;
import com.aimall.product.service.ProductService;
import com.aimall.product.vo.ProductDetailResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 后台商品 Controller 契约测试：验证 JSON、校验和 HTTP 状态码。 */
class ProductControllerTest {
    private final ProductService service = mock(ProductService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProductController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    /** 查询详情会把 Service 的商品字段序列化为响应 JSON。 */
    void getProductReturnsDetail() throws Exception {
        when(service.get(1L)).thenReturn(new ProductDetailResponse(1L, "SKU-1", "商品", new BigDecimal("9.90"),
                2, 1, null, null, LocalDateTime.now(), LocalDateTime.now()));
        mvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sku").value("SKU-1"));
    }

    @Test
    /** 空商品请求体被参数校验拦截，Service 不会收到调用。 */
    void createRejectsInvalidBody() throws Exception {
        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    @Test
    /** 删除成功返回 204，并把路径 ID 传给 Service。 */
    void deleteReturnsNoContent() throws Exception {
        mvc.perform(delete("/api/v1/products/1")).andExpect(status().isNoContent());
        verify(service).delete(1L);
    }

    @Test
    /** Admin 修改库存低于预留数量时必须返回稳定的 409 业务错误码。 */
    void updateReturnsReservedStockConflict() throws Exception {
        when(service.update(eq(1L), any(ProductWriteRequest.class)))
                .thenThrow(new ProductStockConflictException(1L, 3));

        mvc.perform(put("/api/v1/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-1\",\"name\":\"商品\",\"price\":\"9.90\",\"stock\":2,\"status\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_STOCK_CONFLICT"));
    }
}
