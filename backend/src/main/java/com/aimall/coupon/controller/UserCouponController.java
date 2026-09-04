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
/** 当前登录用户的优惠券查询接口，路径中的 me 表示归属由 JWT 决定。 */
public class UserCouponController {
    private final UserCouponService service;
    public UserCouponController(UserCouponService service) { this.service = service; }

    @GetMapping
    /** 分页查询当前用户的可用、已使用或已过期优惠券。 */
    UserCouponPageResponse list(Authentication authentication,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) String status) {
        return service.list(userId(authentication), page, pageSize, status);
    }

    @GetMapping("/{id}")
    /** 查询当前用户的一张券；即使知道别人的 ID 也不能读取。 */
    UserCouponResponse get(Authentication authentication, @PathVariable @Positive Long id) {
        return service.get(userId(authentication), id);
    }

    private Long userId(Authentication authentication) {
        // 券归属只能来自已验证 JWT，绝不接受客户端传入用户 ID。
        return Long.valueOf(authentication.getPrincipal().toString());
    }
}
