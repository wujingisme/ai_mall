package com.aimall.coupon.vo;

import java.util.List;

public record UserCouponPageResponse(List<UserCouponResponse> items, long page, long pageSize,
        long total, long totalPages) {}
