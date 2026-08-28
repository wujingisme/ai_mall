package com.aimall.shop.vo;

import java.math.BigDecimal;

/** 消费端商品详情不返回 SKU、精确库存和管理状态，避免泄露后台经营数据。 */
public record ShopProductDetailResponse(
        Long id,
        String name,
        BigDecimal price,
        String imageUrl,
        String description,
        boolean soldOut
) {}
