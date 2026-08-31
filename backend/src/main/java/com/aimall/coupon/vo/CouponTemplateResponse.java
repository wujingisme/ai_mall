package com.aimall.coupon.vo;

import java.time.OffsetDateTime;

public record CouponTemplateResponse(String id, String name, String couponType,
        String minimumSpend, String discountAmount, Integer totalQuantity, Integer issuedQuantity,
        Integer perUserLimit, String validityType, OffsetDateTime validFrom, OffsetDateTime validUntil,
        Integer validDays, Boolean shareEnabled, String status,
        OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
