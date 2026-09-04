package com.aimall.auth.vo;

import java.util.List;

/** 后台用户管理分页响应。 */
public record AdminUserPageResponse(List<AdminUserResponse> items, long page, long pageSize,
        long total, long totalPages) {}
