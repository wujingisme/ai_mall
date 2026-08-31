package com.aimall.coupon.controller;

import com.aimall.coupon.service.UserCouponService;
import com.aimall.coupon.vo.*;
import jakarta.validation.constraints.*;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/me/coupons")
public class UserCouponController {
    private final UserCouponService service;
    public UserCouponController(UserCouponService service) { this.service = service; }

    @GetMapping
    UserCouponPageResponse list(Authentication authentication,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) String status) {
        return service.list(userId(authentication), page, pageSize, status);
    }

    @GetMapping("/{id}")
    UserCouponResponse get(Authentication authentication, @PathVariable @Positive Long id) {
        return service.get(userId(authentication), id);
    }

    private Long userId(Authentication authentication) {
        // 券归属只能来自已验证 JWT，绝不接受客户端传入用户 ID。
        return Long.valueOf(authentication.getPrincipal().toString());
    }
}
