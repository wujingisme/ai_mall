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
public class CouponGrantController {
    private final CouponGrantService service;
    public CouponGrantController(CouponGrantService service) { this.service = service; }

    @PostMapping
    ResponseEntity<CouponGrantResponse> grant(Authentication authentication,
            @Valid @RequestBody CouponGrantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.grant(Long.valueOf(authentication.getPrincipal().toString()), request));
    }
}
