package com.aimall.cart.vo;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(List<CartItemResponse> items, int totalQuantity, BigDecimal totalAmount) {
}
