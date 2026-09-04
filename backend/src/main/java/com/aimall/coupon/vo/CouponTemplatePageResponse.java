package com.aimall.coupon.vo;

import java.util.List;

/** 优惠券模板分页响应。 */
public record CouponTemplatePageResponse(List<CouponTemplateResponse> items,
        long page, long pageSize, long total, long totalPages) {}
