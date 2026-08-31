package com.aimall.coupon.vo;

import java.time.OffsetDateTime;

public record CouponGrantResponse(String id, String templateId, String targetUserId, String operatorUserId,
        int requestedQuantity, int successQuantity, String reason, String idempotencyKey,
        String status, OffsetDateTime createdAt) {}
