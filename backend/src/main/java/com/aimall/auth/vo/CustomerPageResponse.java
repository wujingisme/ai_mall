package com.aimall.auth.vo;

import java.util.List;

public record CustomerPageResponse(List<CustomerSummaryResponse> items, long page, long pageSize,
        long total, long totalPages) {}
