package com.aimall.order.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** 我的订单详情；取货码明文不会在详情查询中重复返回。 */
public record OrderDetailResponse(
        String id,
        String orderNo,
        String status,
        String pickupLocationName,
        String pickupLocationAddress,
        int itemQuantity,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        OffsetDateTime createdAt,
        OffsetDateTime cancelledAt,
        OffsetDateTime pickedUpAt) {
}
