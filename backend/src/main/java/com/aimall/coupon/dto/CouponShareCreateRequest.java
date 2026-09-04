package com.aimall.coupon.dto;
import jakarta.validation.constraints.*;
/** 创建分享凭证的请求，只提交当前用户自己的优惠券实例 ID。 */
public record CouponShareCreateRequest(@NotNull @Positive Long userCouponId) {}
