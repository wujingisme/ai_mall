package com.aimall.order.service;

import com.aimall.cart.entity.CartItem;
import com.aimall.cart.mapper.CartItemMapper;
import com.aimall.order.dto.OrderPreviewItemRequest;
import com.aimall.order.dto.OrderPreviewRequest;
import com.aimall.order.entity.MallOrder;
import com.aimall.order.entity.OrderItem;
import com.aimall.order.exception.OrderRuleException;
import com.aimall.order.exception.OrderStockInsufficientException;
import com.aimall.order.mapper.MallOrderMapper;
import com.aimall.order.mapper.OrderItemMapper;
import com.aimall.product.entity.Product;
import com.aimall.product.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 订单 Service 单元测试。
 *
 * <p>这里不启动 Spring，也不连接 MySQL，而是用 Mock Mapper 模拟数据库返回值。
 * 这样可以把“价格必须以后端商品表为准”“购物车归属”“库存不足”和“重复商品”等
 * 业务规则单独测清楚；Controller 的 HTTP 校验会在另一组测试中覆盖。</p>
 */
class OrderServiceTest {
    private final CartItemMapper cartItemMapper = mock(CartItemMapper.class);
    private final ProductMapper productMapper = mock(ProductMapper.class);
    private final MallOrderMapper orderMapper = mock(MallOrderMapper.class);
    private final OrderItemMapper orderItemMapper = mock(OrderItemMapper.class);
    private final OrderService service = new OrderService(
            cartItemMapper, productMapper, orderMapper, orderItemMapper,
            "门店取货点", "上海市浦东新区示例路 1 号");

    @BeforeAll
    /** 初始化 MyBatis-Plus 的实体元数据，让 LambdaQueryWrapper 能解析字段方法引用。 */
    static void initializeMybatisMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "order-test-cart"), CartItem.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "order-test-product"), Product.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "order-test-order"), MallOrder.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "order-test-item"), OrderItem.class);
    }

    @Test
    /** 预览金额必须读取商品表的最新价格，并正确计算行金额、总件数和总金额。 */
    void previewCalculatesLatestPriceAndPickupInfo() {
        CartItem cartItem = cartItem(1L, 7L, 5);
        Product product = product(7L, "AI-001", "智能助手", "12.30", 20, 1);
        when(cartItemMapper.selectOne(any())).thenReturn(cartItem);
        when(productMapper.selectById(7L)).thenReturn(product);

        var response = service.preview(99L, new OrderPreviewRequest(
                List.of(new OrderPreviewItemRequest(7L, 2))));

        assertEquals(1, response.items().size());
        assertEquals("7", response.items().get(0).productId());
        assertEquals(new BigDecimal("12.30"), response.items().get(0).unitPrice());
        assertEquals(new BigDecimal("24.60"), response.items().get(0).lineAmount());
        assertEquals(2, response.totalQuantity());
        assertEquals(new BigDecimal("24.60"), response.totalAmount());
        assertEquals("门店取货点", response.pickupLocationName());
        assertEquals("上海市浦东新区示例路 1 号", response.pickupLocationAddress());
    }

    @Test
    /** 同一商品重复传入会被拒绝，避免前端勾选逻辑异常造成重复计价。 */
    void previewRejectsDuplicateProducts() {
        var request = new OrderPreviewRequest(List.of(
                new OrderPreviewItemRequest(7L, 1),
                new OrderPreviewItemRequest(7L, 2)));

        assertThrows(OrderRuleException.class, () -> service.preview(99L, request));
        verifyNoInteractions(cartItemMapper, productMapper);
    }

    @Test
    /** 请求数量大于购物车当前数量时拒绝预览，提示前端先刷新购物车。 */
    void previewRejectsQuantityBeyondCart() {
        when(cartItemMapper.selectOne(any())).thenReturn(cartItem(1L, 7L, 1));

        assertThrows(OrderRuleException.class, () -> service.preview(99L,
                new OrderPreviewRequest(List.of(new OrderPreviewItemRequest(7L, 2)))));
        verifyNoInteractions(productMapper);
    }

    @Test
    /** 商品库存不足返回专门的冲突异常，Controller 会把它转换成 HTTP 409。 */
    void previewRejectsInsufficientStock() {
        when(cartItemMapper.selectOne(any())).thenReturn(cartItem(1L, 7L, 5));
        when(productMapper.selectById(7L)).thenReturn(product(7L, "AI-001", "智能助手", "12.30", 1, 1));

        assertThrows(OrderStockInsufficientException.class, () -> service.preview(99L,
                new OrderPreviewRequest(List.of(new OrderPreviewItemRequest(7L, 2)))));
    }

    @Test
    /** 订单详情查询会按用户 ID 限制数据归属；查不到时由 Service 抛出 404 语义异常。 */
    void getHidesOrdersBelongingToOtherUsers() {
        when(orderMapper.selectOne(any())).thenReturn(null);

        assertThrows(com.aimall.order.exception.OrderNotFoundException.class,
                () -> service.get(99L, 123L));
    }

    @Test
    /** 订单列表只返回主表摘要，并按数据库时间转换成带时区的 API 时间。 */
    void listReturnsSummariesForCurrentUser() {
        MallOrder order = order(123L, "AM202609040001", OrderService.STATUS_PENDING_PICKUP);
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<MallOrder>(2, 10);
        page.setRecords(List.of(order));
        page.setTotal(1);
        when(orderMapper.selectPage(any(), any())).thenReturn(page);

        var response = service.list(99L, 2, 10, " pending_pickup ");

        assertEquals(1, response.items().size());
        assertEquals("123", response.items().get(0).id());
        assertEquals("AM202609040001", response.items().get(0).orderNo());
        assertEquals(OrderService.STATUS_PENDING_PICKUP, response.items().get(0).status());
        assertEquals(2, response.items().get(0).itemQuantity());
        assertEquals(new BigDecimal("24.60"), response.items().get(0).totalAmount());
        assertEquals(1, response.total());
    }

    @Test
    /** 状态筛选使用白名单；非法状态不会访问数据库，避免出现含义不清的查询。 */
    void listRejectsUnknownStatus() {
        assertThrows(OrderRuleException.class, () -> service.list(99L, 1, 20, "PAYING"));
        verifyNoInteractions(orderMapper);
    }

    @Test
    /** 订单详情返回商品快照，即使商品当前信息已经变化也不重新读取 product 表。 */
    void getReturnsItemSnapshots() {
        MallOrder order = order(123L, "AM202609040001", OrderService.STATUS_PICKED_UP);
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrderId(123L);
        item.setProductId(7L);
        item.setSku("AI-001");
        item.setProductName("下单时名称");
        item.setUnitPrice(new BigDecimal("12.30"));
        item.setQuantity(2);
        item.setLineAmount(new BigDecimal("24.60"));
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

        var response = service.get(99L, 123L);

        assertEquals("PICKED_UP", response.status());
        assertEquals(1, response.items().size());
        assertEquals("下单时名称", response.items().get(0).productName());
        assertEquals(new BigDecimal("24.60"), response.items().get(0).lineAmount());
        assertNull(response.cancelledAt());
    }

    private CartItem cartItem(Long id, Long productId, int quantity) {
        CartItem item = new CartItem();
        item.setId(id);
        item.setUserId(99L);
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }

    private Product product(Long id, String sku, String name, String price, int stock, int status) {
        Product product = new Product();
        product.setId(id);
        product.setSku(sku);
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setStatus(status);
        return product;
    }

    private MallOrder order(Long id, String orderNo, String status) {
        MallOrder order = new MallOrder();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setUserId(99L);
        order.setStatus(status);
        order.setPickupLocationName("门店取货点");
        order.setPickupLocationAddress("上海市浦东新区示例路 1 号");
        order.setItemQuantity(2);
        order.setTotalAmount(new BigDecimal("24.60"));
        order.setCreatedAt(LocalDateTime.of(2026, 9, 4, 15, 0));
        order.setPickedUpAt(OrderService.STATUS_PICKED_UP.equals(status)
                ? LocalDateTime.of(2026, 9, 4, 16, 0) : null);
        return order;
    }
}
