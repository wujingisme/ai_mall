package com.aimall.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 修改购物车请求；quantity 表示修改后的最终数量。 */
public record CartItemQuantityRequest(@NotNull @Min(1) @Max(99) Integer quantity) {
}
