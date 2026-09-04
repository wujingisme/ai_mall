package com.aimall.order.controller;

import com.aimall.order.service.AdminOrderService;
import com.aimall.order.vo.AdminOrderDetailResponse;
import com.aimall.order.vo.AdminOrderPageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台订单查询入口。
 *
 * <p>URL 位于 {@code /admin} 命名空间，SecurityConfig 会要求 ADMIN、OPERATOR 或
 * SUPER_ADMIN 角色；Controller 不接收 userId 身份作为权限依据，权限由服务器 JWT
 * 和统一安全配置决定。这里本阶段只开放查询，核销会使用下一阶段的 POST 接口。</p>
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
}
