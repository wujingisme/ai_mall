package com.aimall.auth.vo;

import java.util.List;

/** 客户摘要分页响应。 */
public record CustomerPageResponse(List<CustomerSummaryResponse> items, long page, long pageSize,
        long total, long totalPages) {}
