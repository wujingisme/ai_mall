package com.aimall.product.controller;

import com.aimall.common.exception.GlobalExceptionHandler;
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

class ProductControllerTest {
    private final ProductService service = mock(ProductService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProductController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void getProductReturnsDetail() throws Exception {
        when(service.get(1L)).thenReturn(new ProductDetailResponse(1L, "SKU-1", "商品", new BigDecimal("9.90"),
                2, 1, null, null, LocalDateTime.now(), LocalDateTime.now()));
        mvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sku").value("SKU-1"));
    }

    @Test
    void createRejectsInvalidBody() throws Exception {
        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mvc.perform(delete("/api/v1/products/1")).andExpect(status().isNoContent());
        verify(service).delete(1L);
    }
}
