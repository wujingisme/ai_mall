package com.aimall.coupon.vo;

import java.time.OffsetDateTime;

/** 用户券响应；status 是 Service 根据数据库状态和当前时间派生的展示状态。 */
public record UserCouponResponse(String id, String templateId, String name, String couponType,
        String minimumSpend, String discountAmount, OffsetDateTime validFrom, OffsetDateTime validUntil,
        String status, String source, OffsetDateTime usedAt, OffsetDateTime createdAt) {}
