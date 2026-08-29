package com.aimall.cart.exception;

public class CartItemNotFoundException extends RuntimeException {
    public CartItemNotFoundException(Long productId) {
        super("购物车中不存在该商品：" + productId);
    }
}
