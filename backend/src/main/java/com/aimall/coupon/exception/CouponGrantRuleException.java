package com.aimall.coupon.exception;

/** 发券请求参数或目标用户规则不合法，映射为 HTTP 400。 */
public class CouponGrantRuleException extends RuntimeException {
    public CouponGrantRuleException(String message) { super(message); }
}
