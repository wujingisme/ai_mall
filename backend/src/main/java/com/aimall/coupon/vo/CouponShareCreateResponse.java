package com.aimall.coupon.vo;
import java.time.OffsetDateTime;
public record CouponShareCreateResponse(String shareToken,String sharePath,OffsetDateTime expiresAt) {}
