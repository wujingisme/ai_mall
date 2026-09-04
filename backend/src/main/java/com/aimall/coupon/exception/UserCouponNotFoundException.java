package com.aimall.coupon.exception;

/** 用户优惠券不存在或不属于当前用户，映射为 HTTP 404。 */
public class UserCouponNotFoundException extends RuntimeException {
    public UserCouponNotFoundException(Long id) { super("用户优惠券不存在：" + id); }
}
