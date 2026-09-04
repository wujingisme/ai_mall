package com.aimall.order.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 我的订单列表中的摘要，不返回取货码哈希或内部字段。 */
public record OrderSummaryResponse(
        String id,
        String orderNo,
        String status,
        String pickupLocationName,
        int itemQuantity,
        BigDecimal totalAmount,
        OffsetDateTime createdAt) {
}
