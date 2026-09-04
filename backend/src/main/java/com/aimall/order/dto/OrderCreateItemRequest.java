package com.aimall.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 正式创建订单时选择的一行购物车商品。
 *
 * <p>客户端只能提交商品 ID 和数量；价格、名称、SKU、库存和金额全部由后端读取并保存快照。</p>
 */
public record OrderCreateItemRequest(
        @NotNull(message = "商品不能为空") @Positive(message = "商品 ID 不合法") Long productId,
        @NotNull(message = "购买数量不能为空") @Positive(message = "购买数量必须大于 0")
        @Max(value = 99, message = "单件商品最多购买 99 件") Integer quantity) {
}
