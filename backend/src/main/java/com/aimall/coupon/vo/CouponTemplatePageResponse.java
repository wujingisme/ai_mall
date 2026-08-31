package com.aimall.coupon.vo;

import java.util.List;

public record CouponTemplatePageResponse(List<CouponTemplateResponse> items,
        long page, long pageSize, long total, long totalPages) {}
