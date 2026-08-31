package com.aimall.auth.controller;

import com.aimall.auth.service.AdminCustomerService;
import com.aimall.auth.vo.CustomerPageResponse;
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
}
