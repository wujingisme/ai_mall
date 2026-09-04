package com.aimall.cart.exception;

/** 当前用户购物车中找不到指定商品，映射为 HTTP 404。 */
public class CartItemNotFoundException extends RuntimeException {
    public CartItemNotFoundException(Long productId) {
        super("购物车中不存在该商品：" + productId);
    }
}
