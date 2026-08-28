package com.aimall.shop.vo;

import java.util.List;

public record ShopProductPageResponse(
        List<ShopProductListItemResponse> items,
        long page,
        long pageSize,
        long total,
        long totalPages
) {}
