package com.aimall.coupon.vo;
import java.time.OffsetDateTime;
public record CouponShareResolveResponse(String name,String minimumSpend,String discountAmount,OffsetDateTime expiresAt,boolean claimable) {}
