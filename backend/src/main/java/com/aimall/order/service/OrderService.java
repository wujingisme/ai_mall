package com.aimall.order.service;

import com.aimall.cart.entity.CartItem;
import com.aimall.cart.mapper.CartItemMapper;
import com.aimall.common.exception.ProductNotFoundException;
import com.aimall.order.dto.OrderPreviewItemRequest;
import com.aimall.order.dto.OrderPreviewRequest;
import com.aimall.order.entity.MallOrder;
import com.aimall.order.entity.OrderItem;
import com.aimall.order.exception.OrderNotFoundException;
import com.aimall.order.exception.OrderRuleException;
import com.aimall.order.exception.OrderStockInsufficientException;
import com.aimall.order.mapper.MallOrderMapper;
import com.aimall.order.mapper.OrderItemMapper;
import com.aimall.order.vo.OrderDetailResponse;
import com.aimall.order.vo.OrderItemResponse;
import com.aimall.order.vo.OrderPageResponse;
import com.aimall.order.vo.OrderPreviewItemResponse;
import com.aimall.order.vo.OrderPreviewResponse;
import com.aimall.order.vo.OrderSummaryResponse;
import com.aimall.product.entity.Product;
import com.aimall.product.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 订单基础业务层。
 *
 * <p>第 1 阶段只实现“预览”和“我的订单只读查询”。预览会重新读取购物车和商品表，
 * 但不会写订单、不会锁库存；真正创建订单和库存预留放在后续阶段。</p>
 */
@Service
public class OrderService {
    /** 待取货订单状态；第 2 阶段创建订单时使用。 */
    public static final String STATUS_PENDING_PICKUP = "PENDING_PICKUP";
    /** 已取货订单状态。 */
    public static final String STATUS_PICKED_UP = "PICKED_UP";
    /** 已取消订单状态。 */
    public static final String STATUS_CANCELLED = "CANCELLED";

    private static final Set<String> STATUSES = Set.of(
            STATUS_PENDING_PICKUP, STATUS_PICKED_UP, STATUS_CANCELLED);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final MallOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final String pickupLocationName;
    private final String pickupLocationAddress;

    public OrderService(
            CartItemMapper cartItemMapper,
            ProductMapper productMapper,
            MallOrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            @Value("${mall.pickup.location-name:AI Mall 默认取货点}") String pickupLocationName,
            @Value("${mall.pickup.location-address:请在后台配置线下取货地址}") String pickupLocationAddress) {
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.pickupLocationName = pickupLocationName;
        this.pickupLocationAddress = pickupLocationAddress;
    }

    /**
     * 计算订单预览。
     *
     * <p>前端传来的价格和总额全部被忽略。每个商品都必须属于当前用户购物车，
     * 然后以数据库中的最新价格、上架状态和库存计算结果。</p>
     */
    public OrderPreviewResponse preview(Long userId, OrderPreviewRequest request) {
        // LinkedHashMap 保留前端选择顺序，同时用 key 检测重复商品，避免一个商品被重复计价。
        Map<Long, Integer> requestedItems = new LinkedHashMap<>();
        for (OrderPreviewItemRequest item : request.items()) {
            if (requestedItems.putIfAbsent(item.productId(), item.quantity()) != null) {
                throw new OrderRuleException("同一商品不能在订单预览中重复出现");
            }
        }

        List<OrderPreviewItemResponse> responseItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (Map.Entry<Long, Integer> selected : requestedItems.entrySet()) {
            Long productId = selected.getKey();
            int quantity = selected.getValue();

            // 先验证购物车归属；否则用户可以用预览接口购买任意未加入购物车的商品。
            CartItem cartItem = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                    .eq(CartItem::getUserId, userId)
                    .eq(CartItem::getProductId, productId));
            if (cartItem == null) {
                throw new OrderRuleException("商品不在当前用户购物车中：" + productId);
            }
            if (quantity > cartItem.getQuantity()) {
                throw new OrderRuleException("购物车数量已经变化，请刷新后重试");
            }

            // 商品可能在加入购物车后下架或调价，所以结算时必须重新读取商品表。
            Product product = productMapper.selectById(productId);
            if (product == null) {
                throw new ProductNotFoundException(productId);
            }
            if (!Integer.valueOf(1).equals(product.getStatus())) {
                throw new OrderRuleException("商品已下架，请从购物车移除后重试");
            }
            int currentStock = product.getStock() == null ? 0 : product.getStock();
            if (quantity > currentStock) {
                throw new OrderStockInsufficientException(
                        "商品“" + product.getName() + "”库存不足，仅剩 " + currentStock + " 件");
            }

            BigDecimal lineAmount = product.getPrice()
                    .multiply(BigDecimal.valueOf(quantity))
                    .setScale(2);
            responseItems.add(new OrderPreviewItemResponse(
                    product.getId().toString(), product.getName(), product.getPrice(),
                    quantity, lineAmount, currentStock, true));
            totalQuantity += quantity;
            totalAmount = totalAmount.add(lineAmount);
        }

        return new OrderPreviewResponse(
                responseItems,
                pickupLocationName,
                pickupLocationAddress,
                totalQuantity,
                totalAmount.setScale(2));
    }

    /** 查询当前用户的订单摘要分页；订单状态筛选值由后端白名单校验。 */
    public OrderPageResponse list(Long userId, int page, int pageSize, String status) {
        String normalizedStatus = normalizeStatus(status);
        LambdaQueryWrapper<MallOrder> query = new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getUserId, userId)
                .eq(normalizedStatus != null, MallOrder::getStatus, normalizedStatus)
                .orderByDesc(MallOrder::getCreatedAt)
                .orderByDesc(MallOrder::getId);
        Page<MallOrder> result = orderMapper.selectPage(Page.of(page, pageSize), query);
        return new OrderPageResponse(
                result.getRecords().stream().map(this::toSummary).toList(),
                result.getCurrent(), result.getSize(), result.getTotal(), result.getPages());
    }

    /** 查询当前用户的订单详情，并加载该订单的商品快照明细。 */
    public OrderDetailResponse get(Long userId, Long orderId) {
        MallOrder order = orderMapper.selectOne(new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getId, orderId)
                .eq(MallOrder::getUserId, userId));
        if (order == null) {
            // 不区分“订单不存在”和“订单属于别人”，避免泄露订单是否存在。
            throw new OrderNotFoundException(orderId);
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getId));
        return toDetail(order, items);
    }

    /** 仅允许预定义状态，避免前端传入任意字符串导致筛选语义不明确。 */
    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        // 使用 Locale.ROOT 做协议值规范化，避免服务器系统语言（例如土耳其语）影响英文状态常量。
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw new OrderRuleException("订单状态筛选不合法");
        }
        return normalized;
    }

    /** 把数据库订单转换成列表公开字段，隐藏取货码哈希、用户 ID 等内部字段。 */
    private OrderSummaryResponse toSummary(MallOrder order) {
        return new OrderSummaryResponse(
                order.getId().toString(), order.getOrderNo(), order.getStatus(),
                order.getPickupLocationName(), order.getItemQuantity(), order.getTotalAmount(),
                toOffset(order.getCreatedAt()));
    }

    /** 把数据库订单和明细快照转换成详情公开字段。 */
    private OrderDetailResponse toDetail(MallOrder order, List<OrderItem> items) {
        return new OrderDetailResponse(
                order.getId().toString(), order.getOrderNo(), order.getStatus(),
                order.getPickupLocationName(), order.getPickupLocationAddress(),
                order.getItemQuantity(), order.getTotalAmount(),
                items.stream().map(this::toItemResponse).toList(),
                toOffset(order.getCreatedAt()), toOffset(order.getCancelledAt()),
                toOffset(order.getPickedUpAt()));
    }

    /** 将订单明细快照转换为前端可展示的字段。 */
    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId() == null ? null : item.getProductId().toString(),
                item.getSku(), item.getProductName(), item.getUnitPrice(),
                item.getQuantity(), item.getLineAmount());
    }

    /** 数据库使用本地时间；API 统一转为带 Asia/Shanghai 偏移量的时间。 */
    private OffsetDateTime toOffset(LocalDateTime value) {
        return value == null ? null : value.atZone(BUSINESS_ZONE).toOffsetDateTime();
    }
}
