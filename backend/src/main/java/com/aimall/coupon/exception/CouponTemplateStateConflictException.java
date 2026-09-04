package com.aimall.coupon.exception;

/** 模板状态不允许当前操作，映射为 HTTP 409。 */
public class CouponTemplateStateConflictException extends RuntimeException {
    public CouponTemplateStateConflictException(String message) { super(message); }
}
