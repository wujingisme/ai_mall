package com.aimall.order.service;

import com.aimall.auth.entity.MallUser;
import com.aimall.auth.mapper.MallUserMapper;
import com.aimall.order.entity.MallOrder;
import com.aimall.order.entity.OrderItem;
import com.aimall.order.exception.OrderNotFoundException;
import com.aimall.order.exception.OrderRuleException;
import com.aimall.order.mapper.MallOrderMapper;
import com.aimall.order.mapper.OrderItemMapper;
import com.aimall.order.vo.AdminOrderDetailResponse;
import com.aimall.order.vo.AdminOrderPageResponse;
import com.aimall.order.vo.AdminOrderSummaryResponse;
import com.aimall.order.vo.OrderItemResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 后台订单查询业务层。
 *
 * <p>消费者的“我的订单”必须从 JWT 取 userId；后台查询则允许管理角色按订单号、
 * 客户 ID 和状态查看全部订单。因此这里单独建立 Admin Service，而不是复用消费者
 * Service 的归属条件，避免把“后台能查全部”和“用户只能查自己”混成一个容易越权的入口。</p>
 *
 * <p>本阶段只读订单和商品快照，不修改订单状态，也不触碰库存。取货核销会在下一阶段
 * 使用独立的事务方法实现，便于单独测试状态转换和库存结算。</p>
 */
@Service
public class AdminOrderService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Set<String> STATUSES = Set.of(
            OrderService.STATUS_PENDING_PICKUP,
            OrderService.STATUS_PICKED_UP,
            OrderService.STATUS_CANCELLED);

    private final MallOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final MallUserMapper userMapper;

    public AdminOrderService(
            MallOrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            MallUserMapper userMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.userMapper = userMapper;
    }

    /**
     * 分页查询后台订单。
     *
     * <p>查询条件全部在数据库分页之前加入，避免先把全量订单读入内存再筛选；
     * 客户信息使用一次批量查询补齐，避免列表中每一行订单都单独查一次用户表造成 N+1 查询。</p>
     */
    public AdminOrderPageResponse list(
            int page,
            int pageSize,
            String status,
            String orderNo,
            Long userId) {
        String normalizedStatus = normalizeStatus(status);
        String normalizedOrderNo = StringUtils.hasText(orderNo) ? orderNo.trim() : null;

        LambdaQueryWrapper<MallOrder> query = new LambdaQueryWrapper<MallOrder>()
                .eq(normalizedStatus != null, MallOrder::getStatus, normalizedStatus)
                .like(normalizedOrderNo != null, MallOrder::getOrderNo, normalizedOrderNo)
                .eq(userId != null, MallOrder::getUserId, userId)
                .orderByDesc(MallOrder::getCreatedAt)
                .orderByDesc(MallOrder::getId);
        Page<MallOrder> result = orderMapper.selectPage(Page.of(page, pageSize), query);
        Map<Long, MallUser> customers = loadCustomers(result.getRecords());

        List<AdminOrderSummaryResponse> items = result.getRecords().stream()
                .map(order -> toSummary(order, customers.get(order.getUserId())))
                .toList();
        return new AdminOrderPageResponse(
                items,
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getPages());
    }

    /** 查询后台订单详情；订单明细始终读取下单时保存的商品快照。 */
    public AdminOrderDetailResponse get(Long orderId) {
        MallOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new OrderNotFoundException(orderId);
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getId));
        MallUser customer = order.getUserId() == null ? null : userMapper.selectById(order.getUserId());
        return toDetail(order, items, customer);
    }

    /** 状态筛选只接受数据库约定的三种状态，避免后台传入任意字符串产生歧义。 */
    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase(java.util.Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw new OrderRuleException("订单状态筛选不合法");
        }
        return normalized;
    }

    /** 批量加载列表页涉及的客户，保证每页最多访问一次用户表。 */
    private Map<Long, MallUser> loadCustomers(List<MallOrder> orders) {
        List<Long> userIds = orders.stream()
                .map(MallOrder::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, MallUser> customers = new HashMap<>();
        userMapper.selectBatchIds(userIds).forEach(user -> customers.put(user.getId(), user));
        return customers;
    }

    /** 把订单主表和非敏感客户摘要转换为后台列表行。 */
    private AdminOrderSummaryResponse toSummary(MallOrder order, MallUser customer) {
        return new AdminOrderSummaryResponse(
                order.getId().toString(),
                order.getOrderNo(),
                order.getUserId() == null ? null : order.getUserId().toString(),
                customer == null ? null : customer.getUsername(),
                customer == null ? null : customer.getDisplayName(),
                order.getStatus(),
                order.getPickupLocationName(),
                order.getItemQuantity() == null ? 0 : order.getItemQuantity(),
                order.getTotalAmount(),
                toOffset(order.getCreatedAt()));
    }

    /** 把订单详情转换成后台可展示字段，明确排除取货码哈希、密码和微信身份字段。 */
    private AdminOrderDetailResponse toDetail(MallOrder order, List<OrderItem> items, MallUser customer) {
        return new AdminOrderDetailResponse(
                order.getId().toString(),
                order.getOrderNo(),
                order.getUserId() == null ? null : order.getUserId().toString(),
                customer == null ? null : customer.getUsername(),
                customer == null ? null : customer.getDisplayName(),
                order.getStatus(),
                order.getPickupLocationName(),
                order.getPickupLocationAddress(),
                order.getItemQuantity() == null ? 0 : order.getItemQuantity(),
                order.getTotalAmount(),
                items.stream().map(this::toItemResponse).toList(),
                toOffset(order.getCreatedAt()),
                toOffset(order.getCancelledAt()),
                toOffset(order.getPickedUpAt()));
    }

    /** 商品名称、SKU、单价来自 order_item 快照，避免商品后来改名影响历史订单。 */
    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId() == null ? null : item.getProductId().toString(),
                item.getSku(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineAmount());
    }

    /** 数据库保存本地时间，接口统一输出带业务时区偏移量的时间。 */
    private OffsetDateTime toOffset(LocalDateTime value) {
        return value == null ? null : value.atZone(BUSINESS_ZONE).toOffsetDateTime();
    }
}
