package com.aimall.order.vo;

import java.math.BigDecimal;

/** 订单预览中的商品行；价格和小计均由后端重新计算。 */
public record OrderPreviewItemResponse(
        String productId,
        String name,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineAmount,
        int currentStock,
        boolean available) {
}
