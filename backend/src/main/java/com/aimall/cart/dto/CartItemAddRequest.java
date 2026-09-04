package com.aimall.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 加入购物车请求；quantity 表示本次增加量，而不是最终数量。 */
public record CartItemAddRequest(
        @NotNull @Positive Long productId,
        @NotNull @Min(1) @Max(99) Integer quantity) {
}
