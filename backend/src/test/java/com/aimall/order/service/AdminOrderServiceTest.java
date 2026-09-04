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
import com.aimall.product.entity.Product;
import com.aimall.product.mapper.ProductMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Admin 订单查询 Service 测试：验证后台查询范围、客户摘要和订单快照映射。 */
class AdminOrderServiceTest {
    private final MallOrderMapper orderMapper = mock(MallOrderMapper.class);
    private final OrderItemMapper orderItemMapper = mock(OrderItemMapper.class);
    private final MallUserMapper userMapper = mock(MallUserMapper.class);
    private final ProductMapper productMapper = mock(ProductMapper.class);
    private final AdminOrderService service = new AdminOrderService(orderMapper, orderItemMapper, userMapper, productMapper);

    @Test
    /** 列表按页读取订单，并批量补齐客户展示信息而不暴露微信身份字段。 */
    void listMapsOrderAndCustomerSummary() {
        MallOrder order = order(123L, 7L, OrderService.STATUS_PENDING_PICKUP);
        Page<MallOrder> page = new Page<>(1, 20);
        page.setRecords(List.of(order));
        page.setTotal(1);
        MallUser customer = mock(MallUser.class);
        when(customer.getId()).thenReturn(7L);
        when(customer.getUsername()).thenReturn("customer-7");
        when(customer.getDisplayName()).thenReturn("小明");
        when(orderMapper.selectPage(any(), any())).thenReturn(page);
        when(userMapper.selectBatchIds(List.of(7L))).thenReturn(List.of(customer));

        var response = service.list(1, 20, " pending_pickup ", "AM2026", 7L);

        assertEquals(1, response.items().size());
        var item = response.items().get(0);
        assertEquals("123", item.id());
        assertEquals("7", item.userId());
        assertEquals("customer-7", item.username());
        assertEquals("小明", item.displayName());
        assertEquals(OrderService.STATUS_PENDING_PICKUP, item.status());
        verify(userMapper).selectBatchIds(List.of(7L));
    }

    @Test
    /** 详情使用 order_item 商品快照，且响应不会包含取货码哈希。 */
    void getMapsSnapshotDetails() {
        MallOrder order = order(123L, 7L, OrderService.STATUS_CANCELLED);
        order.setPickupCodeHash("must-not-be-returned");
        OrderItem item = new OrderItem();
        item.setId(9L);
        item.setOrderId(123L);
        item.setProductId(3L);
        item.setSku("SKU-3");
        item.setProductName("历史商品名");
        item.setUnitPrice(new BigDecimal("12.30"));
        item.setQuantity(2);
        item.setLineAmount(new BigDecimal("24.60"));
        MallUser customer = mock(MallUser.class);
        when(customer.getUsername()).thenReturn("customer-7");
        when(customer.getDisplayName()).thenReturn("小明");
        when(orderMapper.selectById(123L)).thenReturn(order);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));
        when(userMapper.selectById(7L)).thenReturn(customer);

        var response = service.get(123L);

        assertEquals("123", response.id());
        assertEquals("customer-7", response.username());
        assertEquals("3", response.items().get(0).productId());
        assertEquals("历史商品名", response.items().get(0).productName());
        assertEquals("CANCELLED", response.status());
        // DTO 没有 pickupCodeHash 字段，编译期保证后台详情不会误返回摘要。
    }

    @Test
    /** 不存在订单统一返回 404 语义异常，避免后台页面把空对象当成成功。 */
    void getRejectsMissingOrder() {
        when(orderMapper.selectById(404L)).thenReturn(null);

        assertThrows(OrderNotFoundException.class, () -> service.get(404L));
        verifyNoInteractions(orderItemMapper, userMapper);
    }

    @Test
    /** 正确取货码会在同一业务流程中结算商品库存并把订单改为已取货。 */
    void verifyPickupSettlesStockAndMarksOrderPickedUp() {
        MallOrder order = order(123L, 7L, OrderService.STATUS_PENDING_PICKUP);
        order.setPickupCodeHash(hash("ABCD2345"));
        OrderItem item = item(9L, 123L, 3L, 2);
        Product product = product(3L, 10, 2);
        when(orderMapper.selectForUpdateById(123L)).thenReturn(order);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));
        when(productMapper.selectForUpdate(3L)).thenReturn(product);
        when(productMapper.settlePickedUpStock(3L, 2)).thenReturn(1);
        when(orderMapper.markPickedUp(eq(123L), any(), any())).thenReturn(1);

        var response = service.verifyPickup(123L, "abcd2345");

        assertEquals(OrderService.STATUS_PICKED_UP, response.status());
        verify(productMapper).settlePickedUpStock(3L, 2);
        verify(orderMapper).markPickedUp(eq(123L), any(), any());
    }

    @Test
    /** 错误取货码必须在接触商品库存前拒绝，确保库存和订单状态都不变。 */
    void verifyPickupRejectsWrongCodeBeforeInventory() {
        MallOrder order = order(123L, 7L, OrderService.STATUS_PENDING_PICKUP);
        order.setPickupCodeHash(hash("ABCD2345"));
        when(orderMapper.selectForUpdateById(123L)).thenReturn(order);

        assertThrows(OrderPickupCodeInvalidException.class,
                () -> service.verifyPickup(123L, "ZZZZ9999"));

        verifyNoInteractions(productMapper, orderItemMapper, userMapper);
        verify(orderMapper, never()).markPickedUp(any(), any(), any());
    }

    @Test
    /** 已核销订单用正确取货码重复请求直接返回详情，不再次扣减商品库存。 */
    void verifyPickupIsIdempotentAfterPickedUp() {
        MallOrder order = order(123L, 7L, OrderService.STATUS_PICKED_UP);
        order.setPickupCodeHash(hash("ABCD2345"));
        when(orderMapper.selectForUpdateById(123L)).thenReturn(order);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item(9L, 123L, 3L, 2)));

        var response = service.verifyPickup(123L, "ABCD2345");

        assertEquals(OrderService.STATUS_PICKED_UP, response.status());
        verifyNoInteractions(productMapper);
        verify(orderMapper, never()).markPickedUp(any(), any(), any());
    }

    @Test
    /** 已取消订单不能因为取货码正确而恢复或进入已取货状态。 */
    void verifyPickupRejectsCancelledOrder() {
        MallOrder order = order(123L, 7L, OrderService.STATUS_CANCELLED);
        when(orderMapper.selectForUpdateById(123L)).thenReturn(order);

        assertThrows(OrderStateConflictException.class,
                () -> service.verifyPickup(123L, "ABCD2345"));

        verifyNoInteractions(productMapper, orderItemMapper, userMapper);
    }

    @Test
    /** 商品库存结算失败时不更新订单状态，真实事务会回滚此前可能成功的商品更新。 */
    void verifyPickupRollsBackWhenStockSettlementFails() {
        MallOrder order = order(123L, 7L, OrderService.STATUS_PENDING_PICKUP);
        order.setPickupCodeHash(hash("ABCD2345"));
        when(orderMapper.selectForUpdateById(123L)).thenReturn(order);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item(9L, 123L, 3L, 2)));
        when(productMapper.selectForUpdate(3L)).thenReturn(product(3L, 1, 1));
        when(productMapper.settlePickedUpStock(3L, 2)).thenReturn(0);

        assertThrows(OrderInventoryConflictException.class,
                () -> service.verifyPickup(123L, "ABCD2345"));

        verify(orderMapper, never()).markPickedUp(any(), any(), any());
    }

    @Test
    /** 非法状态筛选在访问数据库前拒绝，避免前端传入未知状态导致错误查询。 */
    void listRejectsUnknownStatus() {
        assertThrows(OrderRuleException.class, () -> service.list(1, 20, "PAYING", null, null));
        verifyNoInteractions(orderMapper, userMapper);
    }

    private MallOrder order(Long id, Long userId, String status) {
        MallOrder order = new MallOrder();
        order.setId(id);
        order.setOrderNo("AM202609040001");
        order.setUserId(userId);
        order.setStatus(status);
        order.setPickupLocationName("默认取货点");
        order.setPickupLocationAddress("取货地址");
        order.setItemQuantity(2);
        order.setTotalAmount(new BigDecimal("24.60"));
        order.setCreatedAt(LocalDateTime.of(2026, 9, 4, 12, 0));
        return order;
    }

    private OrderItem item(Long id, Long orderId, Long productId, int quantity) {
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setSku("SKU-" + productId);
        item.setProductName("商品快照");
        item.setUnitPrice(new BigDecimal("12.30"));
        item.setQuantity(quantity);
        item.setLineAmount(new BigDecimal("24.60"));
        return item;
    }

    private Product product(Long id, int stock, int reservedStock) {
        Product product = new Product();
        product.setId(id);
        product.setStock(stock);
        product.setReservedStock(reservedStock);
        product.setStatus(1);
        return product;
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
