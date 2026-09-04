package com.aimall.order.service;

import com.aimall.cart.entity.CartItem;
import com.aimall.cart.mapper.CartItemMapper;
import com.aimall.common.exception.ProductNotFoundException;
import com.aimall.order.dto.OrderCreateItemRequest;
import com.aimall.order.dto.OrderCreateRequest;
import com.aimall.order.dto.OrderPreviewItemRequest;
import com.aimall.order.dto.OrderPreviewRequest;
import com.aimall.order.entity.MallOrder;
import com.aimall.order.entity.OrderItem;
import com.aimall.order.exception.OrderNotFoundException;
import com.aimall.order.exception.OrderIdempotencyConflictException;
import com.aimall.order.exception.OrderRuleException;
import com.aimall.order.exception.OrderStockInsufficientException;
import com.aimall.order.mapper.MallOrderMapper;
import com.aimall.order.mapper.OrderItemMapper;
import com.aimall.order.vo.OrderDetailResponse;
import com.aimall.order.vo.OrderItemResponse;
import com.aimall.order.vo.OrderPageResponse;
import com.aimall.order.vo.OrderCreateResponse;
import com.aimall.order.vo.OrderPreviewItemResponse;
import com.aimall.order.vo.OrderPreviewResponse;
import com.aimall.order.vo.OrderSummaryResponse;
import com.aimall.product.entity.Product;
import com.aimall.product.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 订单基础业务层。
 *
 * <p>订单创建和预览都以商品表为价格/库存事实来源；创建订单会在单个事务中锁住购物车、
 * 条件增加商品预留库存、写入订单快照并删除已下单购物车条目。</p>
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
    /** 取货码排除容易混淆的 0、1、I、O，方便线下人工核对。 */
    private static final char[] PICKUP_CODE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final MallOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final String pickupLocationName;
    private final String pickupLocationAddress;
    private final SecureRandom secureRandom = new SecureRandom();

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
            // 预览必须使用可售库存；已被其他待取货订单占用的数量不能再次出售。
            int currentStock = availableStock(product);
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

    /**
     * 创建一笔待取货订单。
     *
     * <p>这是订单创建最重要的事务边界：购物车锁、商品行锁、预留库存、订单主表、
     * 订单明细和购物车删除必须全部成功，否则全部回滚。这样不会出现“库存已占用但没有订单”
     * 或“订单已生成但库存没有减少可售量”的半成品状态。</p>
     *
     * <p>幂等查询使用 {@code SELECT ... FOR UPDATE} 锁住用户+幂等键对应的唯一索引范围。
     * 同一个请求并发到达时，后到的请求会等待前一个事务提交，然后复用第一笔订单，
     * 不会再次增加 reserved_stock。</p>
     */
    @Transactional
    public OrderCreateResponse create(Long userId, OrderCreateRequest request) {
        Map<Long, Integer> requestedItems = normalizeCreateItems(request);
        // Controller 会通过 @NotBlank 校验幂等键；Service 再做一次防守性校验，避免未来被其他入口调用时出现空键。
        if (!StringUtils.hasText(request.clientRequestId())) {
            throw new OrderRuleException("幂等键不能为空");
        }
        String idempotencyKey = request.clientRequestId().trim();
        String payloadHash = hashCreateItems(requestedItems);

        // 先锁定幂等键对应的订单，再做库存操作；这样网络重试不会重复占用库存。
        MallOrder existing = findIdempotentOrderForUpdate(userId, idempotencyKey);
        if (existing != null) {
            if (!payloadHash.equals(existing.getIdempotencyPayloadHash())) {
                throw new OrderIdempotencyConflictException();
            }
            List<OrderItem> existingItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, existing.getId())
                    .orderByAsc(OrderItem::getId));
            // 重试只能拿到订单快照；明文取货码只在第一次创建成功时返回。
            return new OrderCreateResponse(toDetail(existing, existingItems), null, true);
        }

        List<OrderItemDraft> drafts = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;

        // Map 已按请求去重；这里再按 productId 升序处理，统一多个并发订单的锁顺序，降低死锁概率。
        for (Long productId : requestedItems.keySet().stream().sorted().toList()) {
            int quantity = requestedItems.get(productId);
            CartItem cartItem = cartItemMapper.selectForUpdate(userId, productId);
            if (cartItem == null) {
                throw new OrderRuleException("商品不在当前用户购物车中：" + productId);
            }
            if (quantity > cartItem.getQuantity()) {
                throw new OrderRuleException("购物车数量已经变化，请刷新后重试");
            }

            // 先锁商品行，再做条件更新；价格、名称和 SKU 快照来自这次锁定读取。
            Product product = productMapper.selectForUpdate(productId);
            if (product == null) {
                throw new ProductNotFoundException(productId);
            }
            if (!Integer.valueOf(1).equals(product.getStatus())) {
                throw new OrderRuleException("商品已下架，请从购物车移除后重试");
            }
            int currentStock = availableStock(product);
            if (quantity > currentStock) {
                throw new OrderStockInsufficientException(
                        "商品“" + product.getName() + "”库存不足，仅剩 " + currentStock + " 件");
            }
            // 即使前面的读取已判断库存，条件更新仍是数据库层的最终并发保护。
            if (productMapper.reserveStock(productId, quantity) != 1) {
                throw new OrderStockInsufficientException(
                        "商品“" + product.getName() + "”库存不足，请刷新后重试");
            }

            BigDecimal lineAmount = product.getPrice()
                    .multiply(BigDecimal.valueOf(quantity)).setScale(2);
            drafts.add(new OrderItemDraft(product, quantity, lineAmount));
            totalQuantity += quantity;
            totalAmount = totalAmount.add(lineAmount);
        }

        LocalDateTime now = LocalDateTime.now();
        String pickupCode = generatePickupCode();
        MallOrder order = new MallOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setStatus(STATUS_PENDING_PICKUP);
        order.setPickupLocationName(pickupLocationName);
        order.setPickupLocationAddress(pickupLocationAddress);
        order.setPickupCodeHash(sha256(pickupCode));
        order.setTotalAmount(totalAmount.setScale(2));
        order.setItemQuantity(totalQuantity);
        order.setIdempotencyKey(idempotencyKey);
        order.setIdempotencyPayloadHash(payloadHash);
        // 显式设置时间，避免数据库默认值尚未回填到实体时创建响应缺少 createdAt。
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        orderMapper.insert(order);

        List<OrderItem> snapshots = new ArrayList<>();
        for (OrderItemDraft draft : drafts) {
            Product product = draft.product();
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setSku(product.getSku());
            item.setProductName(product.getName());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(draft.quantity());
            item.setLineAmount(draft.lineAmount());
            item.setCreatedAt(now);
            orderItemMapper.insert(item);
            snapshots.add(item);
        }

        // 只删除本次提交的商品，用户购物车中未选中的其他商品必须保留。
        for (Long productId : requestedItems.keySet()) {
            cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                    .eq(CartItem::getUserId, userId)
                    .eq(CartItem::getProductId, productId));
        }
        return new OrderCreateResponse(toDetail(order, snapshots), pickupCode, false);
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

    /** 统一订单预览的库存口径；新增字段前的历史对象按 0 预留兼容。 */
    private int availableStock(Product product) {
        int stock = product.getStock() == null ? 0 : product.getStock();
        int reserved = product.getReservedStock() == null ? 0 : product.getReservedStock();
        return Math.max(stock - reserved, 0);
    }

    /** 把创建订单商品去重并保留数量；重复商品拒绝而不是静默合并，避免前端状态异常被掩盖。 */
    private Map<Long, Integer> normalizeCreateItems(OrderCreateRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new OrderRuleException("请至少选择一件商品");
        }
        Map<Long, Integer> requestedItems = new LinkedHashMap<>();
        for (OrderCreateItemRequest item : request.items()) {
            if (item == null || item.productId() == null || item.quantity() == null) {
                throw new OrderRuleException("商品和购买数量不能为空");
            }
            if (requestedItems.putIfAbsent(item.productId(), item.quantity()) != null) {
                throw new OrderRuleException("同一商品不能在订单中重复出现");
            }
        }
        return requestedItems;
    }

    /** 使用排序后的 productId:quantity 生成稳定摘要，避免请求数组顺序不同被误判为不同订单。 */
    private String hashCreateItems(Map<Long, Integer> requestedItems) {
        String canonical = requestedItems.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(","));
        return sha256(canonical);
    }

    /** 锁住同一用户的幂等键；InnoDB 唯一索引范围锁能串行化并发的同键创建请求。 */
    private MallOrder findIdempotentOrderForUpdate(Long userId, String idempotencyKey) {
        return orderMapper.selectOne(new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getUserId, userId)
                .eq(MallOrder::getIdempotencyKey, idempotencyKey)
                .last("FOR UPDATE"));
    }

    /** 订单号只用于展示和人工查询，不承担取货码的安全职责。 */
    private String generateOrderNo() {
        return "AM" + LocalDateTime.now().format(ORDER_NO_TIME) + randomAlphaNumeric(10);
    }

    /** 生成 8 位随机取货码；明文只留在当前方法的返回对象中，数据库只保存摘要。 */
    private String generatePickupCode() {
        StringBuilder code = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            code.append(PICKUP_CODE_ALPHABET[secureRandom.nextInt(PICKUP_CODE_ALPHABET.length)]);
        }
        return code.toString();
    }

    private String randomAlphaNumeric(int length) {
        String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder value = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            value.append(alphabet.charAt(secureRandom.nextInt(alphabet.length())));
        }
        return value.toString();
    }

    /** SHA-256 用于幂等请求摘要和取货码摘要，不保存任何明文凭证。 */
    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("无法生成订单摘要", e);
        }
    }

    /** 创建订单过程中暂存商品快照和金额，订单主表插入后再写入明细。 */
    private record OrderItemDraft(Product product, int quantity, BigDecimal lineAmount) { }
}
