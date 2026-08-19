package com.aimall.product.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductListItemResponse(Long id, String sku, String name, BigDecimal price,
        Integer stock, Integer status, String imageUrl, LocalDateTime createdAt, LocalDateTime updatedAt) {}
