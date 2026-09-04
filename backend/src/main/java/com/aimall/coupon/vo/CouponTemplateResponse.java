package com.aimall.coupon.vo;

import java.time.OffsetDateTime;

/** 模板公开给后台页面的详情；金额以字符串传输，避免 JSON 浮点误差。 */
public record CouponTemplateResponse(String id, String name, String couponType,
        String minimumSpend, String discountAmount, Integer totalQuantity, Integer issuedQuantity,
        Integer perUserLimit, String validityType, OffsetDateTime validFrom, OffsetDateTime validUntil,
        Integer validDays, Boolean shareEnabled, String status,
        OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
