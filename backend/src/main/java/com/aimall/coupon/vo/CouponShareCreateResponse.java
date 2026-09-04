package com.aimall.coupon.vo;
import java.time.OffsetDateTime;
/** 创建分享后的明文 token 只返回一次；sharePath 可直接用于小程序跳转。 */
public record CouponShareCreateResponse(String shareToken,String sharePath,OffsetDateTime expiresAt) {}
