package com.aimall.coupon.exception;

/** 发券幂等键、库存或限领等资源状态冲突，映射为 HTTP 409。 */
public class CouponGrantConflictException extends RuntimeException {
    public CouponGrantConflictException(String message) { super(message); }
}
