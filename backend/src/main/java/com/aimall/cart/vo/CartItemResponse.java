package com.aimall.cart.vo;

import java.math.BigDecimal;

/** 购物车行响应；stock 表示扣除订单预留后的当前可售库存，available 表示当前仍能按该数量购买。 */
public record CartItemResponse(
        Long productId,
        String name,
        BigDecimal price,
        String imageUrl,
        int quantity,
        int stock,
        boolean available) {
}
