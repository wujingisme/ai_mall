package com.aimall.order.exception;

/** 订单当前状态不允许执行请求操作时使用的冲突异常，映射为 HTTP 409。 */
public class OrderStateConflictException extends RuntimeException {
    public OrderStateConflictException(String message) {
        super(message);
    }
}
