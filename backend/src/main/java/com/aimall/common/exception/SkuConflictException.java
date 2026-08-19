package com.aimall.common.exception;

public class SkuConflictException extends RuntimeException {
    public SkuConflictException(String sku) { super("SKU 已存在：" + sku); }
}
