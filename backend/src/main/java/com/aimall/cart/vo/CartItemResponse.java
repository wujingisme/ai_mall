package com.aimall.cart.vo;

import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String name,
        BigDecimal price,
        String imageUrl,
        int quantity,
        int stock,
        boolean available) {
}
