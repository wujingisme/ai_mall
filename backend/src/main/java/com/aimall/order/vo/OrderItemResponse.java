package com.aimall.order.vo;

import java.math.BigDecimal;

/** 历史订单商品明细；展示的是下单时快照。 */
public record OrderItemResponse(
        String productId,
        String sku,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineAmount) {
}
