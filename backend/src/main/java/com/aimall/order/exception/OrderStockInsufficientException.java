package com.aimall.order.exception;

/** 预览时发现商品库存不足；真正下单阶段还会使用数据库条件更新再次防超卖。 */
public class OrderStockInsufficientException extends RuntimeException {
    public OrderStockInsufficientException(String message) {
        super(message);
    }
}
