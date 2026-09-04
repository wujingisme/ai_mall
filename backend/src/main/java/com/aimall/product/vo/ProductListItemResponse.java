package com.aimall.product.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 后台商品列表行响应。 */
public record ProductListItemResponse(Long id, String sku, String name, BigDecimal price,
        Integer stock, Integer status, String imageUrl, LocalDateTime createdAt, LocalDateTime updatedAt) {}
