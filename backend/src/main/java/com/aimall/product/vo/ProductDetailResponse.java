package com.aimall.product.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductDetailResponse(Long id, String sku, String name, BigDecimal price,
        Integer stock, Integer status, String imageUrl, String description,
        LocalDateTime createdAt, LocalDateTime updatedAt) {}
