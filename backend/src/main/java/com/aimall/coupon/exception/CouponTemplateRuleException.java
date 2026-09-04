package com.aimall.coupon.exception;

/** 模板字段或跨字段规则不合法，映射为 HTTP 400。 */
public class CouponTemplateRuleException extends RuntimeException {
    public CouponTemplateRuleException(String message) { super(message); }
}
