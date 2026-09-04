package com.aimall.cart.vo;

import java.math.BigDecimal;
import java.util.List;

/** 购物车整体响应，包含行项目、总数量和可结算金额。 */
public record CartResponse(List<CartItemResponse> items, int totalQuantity, BigDecimal totalAmount) {
}
