package com.aimall.order.vo;

import java.util.List;

/** 我的订单分页响应。 */
public record OrderPageResponse(
        List<OrderSummaryResponse> items,
        long page,
        long pageSize,
        long total,
        long totalPages) {
}
