package com.aimall.coupon.dto;
import jakarta.validation.constraints.*;
public record CouponTokenRequest(@NotBlank @Size(max=128) String shareToken) {}
