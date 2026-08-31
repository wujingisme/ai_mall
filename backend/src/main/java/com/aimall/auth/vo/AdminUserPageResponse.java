package com.aimall.auth.vo;

import java.util.List;

public record AdminUserPageResponse(List<AdminUserResponse> items, long page, long pageSize,
        long total, long totalPages) {}
