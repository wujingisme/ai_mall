package com.aimall.order.exception;

/** 同一个用户的幂等键已经绑定了另一组商品参数。 */
public class OrderIdempotencyConflictException extends RuntimeException {
    public OrderIdempotencyConflictException() {
        super("幂等键已经用于另一笔订单请求，请重新生成幂等键");
    }
}
