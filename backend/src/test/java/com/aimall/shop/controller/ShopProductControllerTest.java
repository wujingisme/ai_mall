package com.aimall.shop.controller;

import com.aimall.shop.service.ShopProductService;
import com.aimall.shop.vo.*;
import com.aimall.config.SecurityConfig;
import com.aimall.auth.service.JwtService;
import com.aimall.auth.mapper.MallUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ShopProductController.class)
@Import(SecurityConfig.class)
/** 消费端商品接口测试：确认匿名可访问且不会泄露后台字段。 */
class ShopProductControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ShopProductService shopProductService;
    @MockitoBean JwtService jwtService;
    @MockitoBean MallUserMapper mallUserMapper;

    @Test
    /** 消费端列表只返回展示字段，不返回 SKU 和精确库存。 */
    void listsConsumerProductsWithoutAdminFields() throws Exception {
        when(shopProductService.list(1, 20, null)).thenReturn(new ShopProductPageResponse(
                List.of(new ShopProductListItemResponse(1L, "示例商品", new BigDecimal("99.00"), null, false)),
                1, 20, 1, 1));

        mvc.perform(get("/api/v1/shop/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("1"))
                .andExpect(jsonPath("$.items[0].name").value("示例商品"))
                .andExpect(jsonPath("$.items[0].sku").doesNotExist())
                .andExpect(jsonPath("$.items[0].stock").doesNotExist());
    }
}
