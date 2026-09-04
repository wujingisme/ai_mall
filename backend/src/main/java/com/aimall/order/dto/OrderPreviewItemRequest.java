package com.aimall.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 订单预览中的一行商品选择。
 *
 * <p>前端只提交商品 ID 和想购买的数量；价格、名称、库存由后端重新读取，
 * 因此不能通过修改请求体伪造订单金额。</p>
 */
public record OrderPreviewItemRequest(
        @NotNull(message = "商品不能为空") @Positive(message = "商品 ID 不合法") Long productId,
        @NotNull(message = "购买数量不能为空") @Positive(message = "购买数量必须大于 0")
        @Max(value = 99, message = "单件商品最多购买 99 件") Integer quantity) {
}
