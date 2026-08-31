package com.aimall.coupon.controller;

import com.aimall.coupon.dto.CouponTemplateWriteRequest;
import com.aimall.coupon.service.CouponTemplateService;
import com.aimall.coupon.vo.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/admin/coupon-templates")
public class CouponTemplateController {
    private final CouponTemplateService service;

    public CouponTemplateController(CouponTemplateService service) { this.service = service; }

    @GetMapping
    CouponTemplatePageResponse list(@RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return service.list(page, pageSize, keyword, status);
    }

    @GetMapping("/{id}")
    CouponTemplateResponse get(@PathVariable @Min(1) Long id) { return service.get(id); }

    @PostMapping
    ResponseEntity<CouponTemplateResponse> create(@Valid @RequestBody CouponTemplateWriteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    CouponTemplateResponse update(@PathVariable @Min(1) Long id,
            @Valid @RequestBody CouponTemplateWriteRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/activation")
    CouponTemplateResponse activate(@PathVariable @Min(1) Long id) { return service.activate(id); }

    @PostMapping("/{id}/deactivation")
    CouponTemplateResponse deactivate(@PathVariable @Min(1) Long id) { return service.deactivate(id); }
}
