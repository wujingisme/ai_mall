package com.aimall.auth.vo;

/** 发券用户选择器使用的非敏感客户摘要。 */
public record CustomerSummaryResponse(String id, String username, String displayName, String avatarUrl) {}
