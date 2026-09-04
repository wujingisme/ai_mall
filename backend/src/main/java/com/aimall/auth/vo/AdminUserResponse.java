package com.aimall.auth.vo;

import java.time.OffsetDateTime;

/** 后台用户管理详情；wechatBound 只表示是否绑定，不返回 OpenID。 */
public record AdminUserResponse(String id, String username, String displayName, String avatarUrl,
        boolean enabled, String roles, boolean wechatBound, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
