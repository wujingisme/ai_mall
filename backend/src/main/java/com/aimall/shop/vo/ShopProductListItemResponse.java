package com.aimall.shop.vo;

import java.math.BigDecimal;

/** 消费端商品卡片，只暴露顾客展示需要的字段。 */
public record ShopProductListItemResponse(
        Long id,
        String name,
        BigDecimal price,
        String imageUrl,
        boolean soldOut
) {}
