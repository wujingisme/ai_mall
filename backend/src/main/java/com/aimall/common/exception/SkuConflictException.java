package com.aimall.common.exception;

/** 商品 SKU 违反唯一约束时的冲突异常。 */
public class SkuConflictException extends RuntimeException {
    public SkuConflictException(String sku) { super("SKU 已存在：" + sku); }
}
