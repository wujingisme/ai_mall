package com.aimall.coupon.exception;

public class UserCouponNotFoundException extends RuntimeException {
    public UserCouponNotFoundException(Long id) { super("用户优惠券不存在：" + id); }
}
