package com.aimall.order.controller;

import com.aimall.order.dto.OrderCreateRequest;
import com.aimall.order.dto.OrderPreviewRequest;
import com.aimall.order.service.OrderService;
import com.aimall.order.vo.OrderDetailResponse;
import com.aimall.order.vo.OrderCreateResponse;
import com.aimall.order.vo.OrderPageResponse;
import com.aimall.order.vo.OrderPreviewResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消费端订单 HTTP 入口。
 *
 * <p>创建、预览和“我的订单”读取都从 JWT 的 Authentication 获取用户 ID，
 * 不从请求体接收，避免客户端修改 ID 后操作或读取别人的订单。</p>
 */
@Validated
@RestController
@RequestMapping("/api/v1")
public class OrderController {
    private final OrderService orderService;

    /** 构造函数注入使 Controller 易于替换 Service 并进行 MockMvc 单元测试。 */
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 计算购物车选中商品的最新价格和取货信息，但不创建订单或锁库存。 */
    @PostMapping("/orders/preview")
    public OrderPreviewResponse preview(
            Authentication authentication,
            @Valid @RequestBody OrderPreviewRequest request) {
        return orderService.preview(userId(authentication), request);
    }

    /**
     * 正式创建线下取货订单。
     *
     * <p>Service 会在事务中锁购物车、预留库存、保存商品快照并删除已下单条目；
     * 响应中的取货码只用于本次创建成功后的展示，详情接口不会重复返回。</p>
     */
    @PostMapping("/orders")
    public OrderCreateResponse create(
            Authentication authentication,
            @Valid @RequestBody OrderCreateRequest request) {
        return orderService.create(userId(authentication), request);
    }

    /**
     * 取消当前用户的一笔待取货订单。
     *
     * <p>请求体不接收新状态或库存数量，Service 会以订单明细快照为准释放预留库存；
     * 已取消订单重复调用保持幂等，已取货订单返回状态冲突。</p>
     */
    @PostMapping("/me/orders/{id}/cancellation")
    public OrderDetailResponse cancel(
            Authentication authentication,
            @PathVariable @Positive Long id) {
        return orderService.cancel(userId(authentication), id);
    }

    /** 查询当前用户订单摘要分页；status 由 Service 按白名单校验。 */
    @GetMapping("/me/orders")
    public OrderPageResponse list(
            Authentication authentication,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) String status) {
        return orderService.list(userId(authentication), page, pageSize, status);
    }

    /** 查询当前用户的单笔订单和商品快照明细。 */
    @GetMapping("/me/orders/{id}")
    public OrderDetailResponse get(
            Authentication authentication,
            @PathVariable @Positive Long id) {
        return orderService.get(userId(authentication), id);
    }

    /** 只从服务端认证上下文读取用户 ID，永远不接受前端提交的 userId。 */
    private Long userId(Authentication authentication) {
        return Long.valueOf(authentication.getPrincipal().toString());
    }
}
