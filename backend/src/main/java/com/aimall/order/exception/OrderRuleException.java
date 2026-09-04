package com.aimall.order.exception;

/** 订单预览参数、购物车归属或商品状态不符合规则时的异常，映射为 HTTP 400。 */
public class OrderRuleException extends RuntimeException {
    public OrderRuleException(String message) {
        super(message);
    }
}
