package com.aimall.auth.controller;

import com.aimall.auth.service.AdminCustomerService;
import com.aimall.auth.vo.CustomerPageResponse;
import com.aimall.auth.vo.AdminUserPageResponse;
import com.aimall.auth.vo.AdminUserResponse;
import com.aimall.coupon.vo.UserCouponPageResponse;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/admin/customers")
public class AdminCustomerController {
    private final AdminCustomerService service;
    public AdminCustomerController(AdminCustomerService service) { this.service = service; }

    @GetMapping
    CustomerPageResponse list(@RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) @Size(max = 100) String keyword) {
        return service.list(page, pageSize, keyword);
    }

    @GetMapping("/manage")
    AdminUserPageResponse listUsers(@RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) Boolean enabled) {
        return service.listUsers(page, pageSize, keyword, enabled);
    }

    @GetMapping("/manage/{id}")
    AdminUserResponse getUser(@PathVariable @Min(1) Long id) { return service.getUser(id); }

    @PostMapping("/manage/{id}/activation")
    AdminUserResponse activate(@PathVariable @Min(1) Long id) { return service.changeEnabled(id, true); }

    @PostMapping("/manage/{id}/deactivation")
    AdminUserResponse deactivate(@PathVariable @Min(1) Long id) { return service.changeEnabled(id, false); }

    @GetMapping("/manage/{id}/coupons")
    UserCouponPageResponse coupons(@PathVariable @Min(1) Long id,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) String status) {
        return service.listCoupons(id, page, pageSize, status);
    }
}
