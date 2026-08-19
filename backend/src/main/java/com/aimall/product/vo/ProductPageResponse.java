package com.aimall.product.vo;

import java.util.List;

public record ProductPageResponse(List<ProductListItemResponse> items, long page, long pageSize,
        long total, long totalPages) {}
