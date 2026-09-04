package com.aimall.order.service;

import com.aimall.auth.entity.MallUser;
import com.aimall.auth.mapper.MallUserMapper;
import com.aimall.order.entity.MallOrder;
import com.aimall.order.entity.OrderItem;
import com.aimall.order.exception.OrderNotFoundException;
import com.aimall.order.exception.OrderRuleException;
import com.aimall.order.mapper.MallOrderMapper;
import com.aimall.order.mapper.OrderItemMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Admin 订单查询 Service 测试：验证后台查询范围、客户摘要和订单快照映射。 */
class AdminOrderServiceTest {
    private final MallOrderMapper orderMapper = mock(MallOrderMapper.class);
    private final OrderItemMapper orderItemMapper = mock(OrderItemMapper.class);
    private final MallUserMapper userMapper = mock(MallUserMapper.class);
    private final AdminOrderService service = new AdminOrderService(orderMapper, orderItemMapper, userMapper);

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
}
