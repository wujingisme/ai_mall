package com.aimall.product.vo;

import java.util.List;

/** 后台商品分页响应。 */
public record ProductPageResponse(List<ProductListItemResponse> items, long page, long pageSize,
        long total, long totalPages) {}
