package com.aimall.coupon.vo;
import java.time.OffsetDateTime;
/** 分享公开预览响应，不包含创建者、用户 ID 或内部券码。 */
public record CouponShareResolveResponse(String name,String minimumSpend,String discountAmount,OffsetDateTime expiresAt,boolean claimable) {}
