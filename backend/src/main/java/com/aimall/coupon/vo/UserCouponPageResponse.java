package com.aimall.coupon.vo;

import java.util.List;

/** 当前用户优惠券分页响应。 */
public record UserCouponPageResponse(List<UserCouponResponse> items, long page, long pageSize,
        long total, long totalPages) {}
