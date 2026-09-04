package com.aimall.product.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 后台商品详情响应，包含编辑页面需要的全部字段。 */
public record ProductDetailResponse(Long id, String sku, String name, BigDecimal price,
        Integer stock, Integer status, String imageUrl, String description,
        LocalDateTime createdAt, LocalDateTime updatedAt) {}
