package com.aimall.coupon.vo;

import java.time.OffsetDateTime;

public record UserCouponResponse(String id, String templateId, String name, String couponType,
        String minimumSpend, String discountAmount, OffsetDateTime validFrom, OffsetDateTime validUntil,
        String status, String source, OffsetDateTime usedAt, OffsetDateTime createdAt) {}
