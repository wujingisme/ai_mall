package com.aimall.order.exception;

/** 取货码与订单摘要不匹配时使用的冲突异常，映射为 HTTP 409。 */
public class OrderPickupCodeInvalidException extends RuntimeException {
    public OrderPickupCodeInvalidException() {
        super("取货码不正确");
    }
}
