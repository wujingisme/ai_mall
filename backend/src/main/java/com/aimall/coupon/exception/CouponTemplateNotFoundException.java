package com.aimall.coupon.exception;

public class CouponTemplateNotFoundException extends RuntimeException {
    public CouponTemplateNotFoundException(Long id) { super("优惠券模板不存在：" + id); }
}
