package com.aimall.auth.vo;

import java.time.OffsetDateTime;

public record AdminUserResponse(String id, String username, String displayName, String avatarUrl,
        boolean enabled, String roles, boolean wechatBound, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
