package com.aimall.order.controller;

import com.aimall.order.dto.PickupVerificationRequest;
import com.aimall.order.service.AdminOrderService;
import com.aimall.order.vo.AdminOrderDetailResponse;
import com.aimall.order.vo.AdminOrderPageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台订单查询入口。
 *
 * <p>URL 位于 {@code /admin} 命名空间，SecurityConfig 会要求 ADMIN、OPERATOR 或
 * SUPER_ADMIN 角色；Controller 不接收 userId 身份作为权限依据，权限由服务器 JWT
 * 和统一安全配置决定。订单查询和取货码核销都放在该后台命名空间下，避免把管理动作
 * 混入消费者自己的订单接口。</p>
 */
@Validated
@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {
    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    /** 按状态、订单号或客户 ID 分页查询全部订单。 */
    @GetMapping
    public AdminOrderPageResponse list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) @Size(max = 24) String status,
            @RequestParam(required = false) @Size(max = 40) String orderNo,
            @RequestParam(required = false) @Min(1) Long userId) {
        return adminOrderService.list(page, pageSize, status, orderNo, userId);
    }

    /** 查询一笔订单的快照详情；不存在的订单由全局异常处理器统一返回 404。 */
    @GetMapping("/{id}")
    public AdminOrderDetailResponse get(@PathVariable @Min(1) Long id) {
        return adminOrderService.get(id);
    }

    /**
     * 接收店员输入的取货码并完成核销。
     *
     * <p>请求体只包含待验证的明文取货码，Controller 不记录、不拼接日志；Service
     * 会在事务中计算摘要、锁订单和商品，并返回核销后的订单详情。</p>
     */
    @PostMapping("/{id}/pickup-verification")
    public ResponseEntity<AdminOrderDetailResponse> verifyPickup(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody PickupVerificationRequest request) {
        return ResponseEntity.ok(adminOrderService.verifyPickup(id, request.pickupCode()));
    }
}
