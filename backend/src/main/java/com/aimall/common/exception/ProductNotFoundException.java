package com.aimall.common.exception;

/** 商品不存在，后台和消费端都使用该统一资源异常。 */
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) { super("商品不存在：" + id); }
}
