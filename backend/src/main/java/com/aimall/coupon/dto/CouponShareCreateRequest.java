package com.aimall.coupon.dto;
import jakarta.validation.constraints.*;
public record CouponShareCreateRequest(@NotNull @Positive Long userCouponId) {}
