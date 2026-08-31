package com.aimall.coupon.exception;
public class CouponShareException extends RuntimeException { private final boolean notFound; public CouponShareException(String message,boolean notFound){super(message);this.notFound=notFound;} public boolean isNotFound(){return notFound;} }
