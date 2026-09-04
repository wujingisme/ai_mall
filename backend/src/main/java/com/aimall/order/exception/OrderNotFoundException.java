package com.aimall.order.exception;

/** 订单不存在或不属于当前用户时使用的资源异常，映射为 HTTP 404。 */
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long id) {
        super("订单不存在：" + id);
    }
}
