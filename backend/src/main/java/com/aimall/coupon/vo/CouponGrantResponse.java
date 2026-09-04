package com.aimall.coupon.vo;

import java.time.OffsetDateTime;

/** 人工发券审计结果，供后台展示操作人、数量、原因和幂等键。 */
public record CouponGrantResponse(String id, String templateId, String targetUserId, String operatorUserId,
        int requestedQuantity, int successQuantity, String reason, String idempotencyKey,
        String status, OffsetDateTime createdAt) {}
