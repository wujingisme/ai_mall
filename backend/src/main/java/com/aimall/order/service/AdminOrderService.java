package com.aimall.order.service;

import com.aimall.auth.entity.MallUser;
import com.aimall.auth.mapper.MallUserMapper;
import com.aimall.order.entity.MallOrder;
import com.aimall.order.entity.OrderItem;
import com.aimall.order.exception.OrderInventoryConflictException;
import com.aimall.order.exception.OrderNotFoundException;
import com.aimall.order.exception.OrderPickupCodeInvalidException;
import com.aimall.order.exception.OrderRuleException;
import com.aimall.order.exception.OrderStateConflictException;
import com.aimall.order.mapper.MallOrderMapper;
import com.aimall.order.mapper.OrderItemMapper;
import com.aimall.order.vo.AdminOrderDetailResponse;
import com.aimall.order.vo.AdminOrderPageResponse;
import com.aimall.order.vo.AdminOrderSummaryResponse;
import com.aimall.order.vo.OrderItemResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aimall.product.entity.Product;
import com.aimall.product.mapper.ProductMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 后台订单查询业务层。
 *
 * <p>消费者的“我的订单”必须从 JWT 取 userId；后台查询则允许管理角色按订单号、
 * 客户 ID 和状态查看全部订单。因此这里单独建立 Admin Service，而不是复用消费者
 * Service 的归属条件，避免把“后台能查全部”和“用户只能查自己”混成一个容易越权的入口。</p>
 *
 * <p>查询方法只读订单和商品快照；取货核销单独使用事务方法，先验证订单状态和取货码，
 * 再按固定顺序锁商品并结算库存，最后才把订单改为已取货。</p>
 */
@Service
public class AdminOrderService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Set<String> STATUSES = Set.of(
            OrderService.STATUS_PENDING_PICKUP,
            OrderService.STATUS_PICKED_UP,
            OrderService.STATUS_CANCELLED);
    /** 与订单创建端一致的 8 位取货码字符集；排除容易混淆的 0、1、I、O。 */
    private static final String PICKUP_CODE_REGEX = "^[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]+$";

    private final MallOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final MallUserMapper userMapper;
    private final ProductMapper productMapper;

    public AdminOrderService(
            MallOrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            MallUserMapper userMapper,
            ProductMapper productMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.userMapper = userMapper;
        this.productMapper = productMapper;
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

    /**
     * 核验线下取货码并完成订单和库存结算。
     *
     * <p>整个方法是一个事务：订单行先加锁，确认状态和取货码后读取订单明细，
     * 再按 productId 升序锁商品并把 stock、reserved_stock 各减少相同数量，
     * 最后才条件更新订单为 PICKED_UP。任何商品不存在、库存不足或状态更新失败，
     * 都会抛出异常并回滚已经执行的库存更新，避免出现“订单已取货但库存没结算”。</p>
     *
     * <p>已经核销的订单在取货码正确时直接返回当前详情，保证店员重复点击不会重复扣库存；
     * 已取消订单拒绝核销。取货码只在内存中规范化和计算摘要，永远不写日志或返回响应。</p>
     */
    @Transactional
    public AdminOrderDetailResponse verifyPickup(Long orderId, String pickupCode) {
        MallOrder order = orderMapper.selectForUpdateById(orderId);
        if (order == null) {
            throw new OrderNotFoundException(orderId);
        }

        if (OrderService.STATUS_CANCELLED.equals(order.getStatus())) {
            throw new OrderStateConflictException("已取消订单不能核销");
        }
        String normalizedCode = normalizePickupCode(pickupCode);
        if (!matchesPickupCode(order, normalizedCode)) {
            throw new OrderPickupCodeInvalidException();
        }

        List<OrderItem> items = loadItems(orderId);
        if (OrderService.STATUS_PICKED_UP.equals(order.getStatus())) {
            // 重复核销只返回当前快照；由于订单行已锁定，不会和第一次核销并发执行。
            return toDetail(order, items, loadCustomer(order.getUserId()));
        }
        if (!OrderService.STATUS_PENDING_PICKUP.equals(order.getStatus())) {
            throw new OrderStateConflictException("当前订单状态不能核销");
        }
        if (items.isEmpty()) {
            // 正常创建订单一定至少有一条明细；历史脏数据不能在没有库存依据时强行核销。
            throw new OrderInventoryConflictException("订单没有商品明细，无法安全结算库存");
        }

        // 聚合相同商品的数量，确保一笔订单中的同一商品只锁一次、只结算一次。
        Map<Long, Integer> settleQuantities = new LinkedHashMap<>();
        for (OrderItem item : items) {
            if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new OrderInventoryConflictException("订单明细缺少有效商品库存信息");
            }
            settleQuantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        for (Long productId : settleQuantities.keySet().stream().sorted().toList()) {
            Product product = productMapper.selectForUpdate(productId);
            if (product == null) {
                throw new OrderInventoryConflictException("订单商品已不存在，无法安全结算库存");
            }
            int quantity = settleQuantities.get(productId);
            if (productMapper.settlePickedUpStock(productId, quantity) != 1) {
                throw new OrderInventoryConflictException("商品库存数量不一致，核销已回滚");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        if (orderMapper.markPickedUp(orderId, now, now) != 1) {
            // 订单行已锁定时通常不会发生；保留条件失败处理作为数据库并发兜底。
            throw new OrderStateConflictException("订单状态已变化，请刷新后重试");
        }
        order.setStatus(OrderService.STATUS_PICKED_UP);
        order.setPickedUpAt(now);
        order.setUpdatedAt(now);
        return toDetail(order, items, loadCustomer(order.getUserId()));
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

    /** 详情和核销都按同一排序读取订单明细，保证快照展示和库存聚合使用相同输入。 */
    private List<OrderItem> loadItems(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getProductId)
                .orderByAsc(OrderItem::getId));
    }

    /** 按 ID 读取客户展示摘要；只读取可展示字段，后续映射仍不会返回敏感身份信息。 */
    private MallUser loadCustomer(Long userId) {
        return userId == null ? null : userMapper.selectById(userId);
    }

    /** 规范化店员输入并做 Service 层防守性校验，避免未来出现绕过 DTO 的调用入口。 */
    private String normalizePickupCode(String pickupCode) {
        if (!StringUtils.hasText(pickupCode)) {
            throw new OrderPickupCodeInvalidException();
        }
        String normalized = pickupCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() != 8 || !normalized.matches(PICKUP_CODE_REGEX)) {
            throw new OrderPickupCodeInvalidException();
        }
        return normalized;
    }

    /** 只比较 SHA-256 摘要，不比较或记录取货码明文；MessageDigest.isEqual 减少时序差异。 */
    private boolean matchesPickupCode(MallOrder order, String pickupCode) {
        String storedHash = order.getPickupCodeHash();
        if (!StringUtils.hasText(storedHash)) {
            return false;
        }
        byte[] expected = storedHash.getBytes(StandardCharsets.UTF_8);
        byte[] actual = sha256(pickupCode).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    /** 与创建订单时一致的 SHA-256 十六进制摘要算法。 */
    private String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("无法计算取货码摘要", e);
        }
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
