package com.aimall.cart.exception;

/** 商品下架、售罄或数量超过库存时的购物车业务冲突。 */
public class CartProductUnavailableException extends RuntimeException {
    public CartProductUnavailableException(String message) { super(message); }
}
