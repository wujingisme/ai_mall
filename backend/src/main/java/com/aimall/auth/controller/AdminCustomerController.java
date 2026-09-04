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
/** 后台客户管理入口：查询 CUSTOMER 用户、查看优惠券并启用/停用账号。 */
public class AdminCustomerController {
    private final AdminCustomerService service;
    public AdminCustomerController(AdminCustomerService service) { this.service = service; }

    @GetMapping
    /** 给人工发券页面提供非敏感的客户候选列表。 */
    CustomerPageResponse list(@RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) @Size(max = 100) String keyword) {
        return service.list(page, pageSize, keyword);
    }

    @GetMapping("/manage")
    /** 提供后台用户管理列表，可按姓名、用户名和启用状态筛选。 */
    AdminUserPageResponse listUsers(@RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) Boolean enabled) {
        return service.listUsers(page, pageSize, keyword, enabled);
    }

    @GetMapping("/manage/{id}")
    /** 查询单个 CUSTOMER 用户；Service 会再次确认目标不是管理员账号。 */
    AdminUserResponse getUser(@PathVariable @Min(1) Long id) { return service.getUser(id); }

    @PostMapping("/manage/{id}/activation")
    /** 启用客户账号。 */
    AdminUserResponse activate(@PathVariable @Min(1) Long id) { return service.changeEnabled(id, true); }

    @PostMapping("/manage/{id}/deactivation")
    /** 停用客户账号，后续 JWT 请求也会被过滤器拒绝。 */
    AdminUserResponse deactivate(@PathVariable @Min(1) Long id) { return service.changeEnabled(id, false); }

    @GetMapping("/manage/{id}/coupons")
    /** 查看指定客户的优惠券，但不允许把任意 userId 传给消费者接口绕过归属校验。 */
    UserCouponPageResponse coupons(@PathVariable @Min(1) Long id,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) String status) {
        return service.listCoupons(id, page, pageSize, status);
    }
}
