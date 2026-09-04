package com.aimall.coupon.dto;
import jakarta.validation.constraints.*;
/** 分享解析/领取共用的随机 token 请求体。 */
public record CouponTokenRequest(@NotBlank @Size(max=128) String shareToken) {}
