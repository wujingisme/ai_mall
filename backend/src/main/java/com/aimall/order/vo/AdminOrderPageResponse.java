package com.aimall.order.vo;

import java.util.List;

/** 后台订单列表的统一分页响应；字段命名与现有 Admin 用户/优惠券分页接口保持一致。 */
public record AdminOrderPageResponse(
        List<AdminOrderSummaryResponse> items,
        long page,
        long pageSize,
        long total,
        long totalPages) {
}
