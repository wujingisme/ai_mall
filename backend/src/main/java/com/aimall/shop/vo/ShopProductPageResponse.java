package com.aimall.shop.vo;

import java.util.List;

/** 消费端商品分页响应。 */
public record ShopProductPageResponse(
        List<ShopProductListItemResponse> items,
        long page,
        long pageSize,
        long total,
        long totalPages
) {}
