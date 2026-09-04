package com.aimall.coupon.exception;

/** 优惠券模板不存在，映射为 HTTP 404。 */
public class CouponTemplateNotFoundException extends RuntimeException {
    public CouponTemplateNotFoundException(Long id) { super("优惠券模板不存在：" + id); }
}
