package com.aimall.coupon.controller;

import com.aimall.coupon.dto.CouponGrantRequest;
import com.aimall.coupon.service.CouponGrantService;
import com.aimall.coupon.vo.CouponGrantResponse;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/coupon-grants")
/** 后台人工发券接口；操作人身份必须来自已验证的 JWT。 */
public class CouponGrantController {
    private final CouponGrantService service;
    public CouponGrantController(CouponGrantService service) { this.service = service; }

    @PostMapping
    /** 发放优惠券并返回审计记录；幂等键避免网络重试重复发券。 */
    ResponseEntity<CouponGrantResponse> grant(Authentication authentication,
            @Valid @RequestBody CouponGrantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.grant(Long.valueOf(authentication.getPrincipal().toString()), request));
    }
}
