package com.aimall.order.vo;

import java.math.BigDecimal;
import java.util.List;

/** 订单预览响应；只读计算结果，不代表订单已经创建。 */
public record OrderPreviewResponse(
        List<OrderPreviewItemResponse> items,
        String pickupLocationName,
        String pickupLocationAddress,
        int totalQuantity,
        BigDecimal totalAmount) {
}
