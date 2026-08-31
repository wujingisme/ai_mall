package com.aimall.coupon.controller;

import com.aimall.coupon.dto.*; import com.aimall.coupon.service.CouponShareService; import com.aimall.coupon.vo.*; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*;

@RestController
public class CouponShareController {
    private final CouponShareService service;
    public CouponShareController(CouponShareService service) { this.service = service; }
    @PostMapping("/api/v1/me/coupon-shares")
    ResponseEntity<CouponShareCreateResponse> create(Authentication a, @Valid @RequestBody CouponShareCreateRequest r) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(Long.valueOf(a.getPrincipal().toString()), r)); }
    @PostMapping("/api/v1/coupon-shares/resolve")
    CouponShareResolveResponse resolve(@Valid @RequestBody CouponTokenRequest r) { return service.resolve(r); }
    @PostMapping("/api/v1/coupon-claims")
    UserCouponResponse claim(Authentication a, @Valid @RequestBody CouponTokenRequest r) { return service.claim(Long.valueOf(a.getPrincipal().toString()), r); }
}
