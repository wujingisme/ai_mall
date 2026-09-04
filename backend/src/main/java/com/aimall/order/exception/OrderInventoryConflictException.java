package com.aimall.order.exception;

/** 订单预留库存无法安全释放时使用的库存一致性异常，映射为 HTTP 409。 */
public class OrderInventoryConflictException extends RuntimeException {
    public OrderInventoryConflictException(String message) {
        super(message);
    }
}
