package com.aimall.cart.service;

import com.aimall.cart.entity.CartItem;
import com.aimall.cart.mapper.CartItemMapper;
import com.aimall.product.entity.Product;
import com.aimall.product.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 购物车库存口径测试。
 *
 * <p>订单预留库存增加后，购物车仍然可以保留商品，但“可售数量”必须使用
 * stock - reservedStock；这里专门验证展示、加入购物车和数量修改不会重新卖出已占用库存。</p>
 */
class CartServiceTest {
    private final CartItemMapper cartItemMapper = mock(CartItemMapper.class);
    private final ProductMapper productMapper = mock(ProductMapper.class);
    private final CartService service = new CartService(cartItemMapper, productMapper);

    @BeforeAll
    /** 初始化 LambdaQueryWrapper 需要的实体元数据。 */
    static void initializeMybatisMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "cart-test-item"), CartItem.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "cart-test-product"), Product.class);
    }

    @Test
    /** 查询购物车返回的 stock 字段代表可售库存，而不是包含预留数量的总库存。 */
    void getReportsAvailableStockAfterReservation() {
        CartItem item = new CartItem();
        item.setId(1L);
        item.setUserId(99L);
        item.setProductId(7L);
        item.setQuantity(2);
        Product product = product(7L, 10, 3, 1);
        when(cartItemMapper.selectList(any())).thenReturn(List.of(item));
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(product));

        var response = service.get(99L);

        assertEquals(7, response.items().get(0).stock());
        assertEquals(true, response.items().get(0).available());
        assertEquals(new BigDecimal("20.00"), response.totalAmount());
    }

    @Test
    /** 只有预留库存时不能再加入购物车，避免绕过订单锁库存。 */
    void addRejectsWhenAllStockIsReserved() {
        when(productMapper.selectById(7L)).thenReturn(product(7L, 10, 10, 1));

        assertThrows(com.aimall.cart.exception.CartProductUnavailableException.class,
                () -> service.add(99L, new com.aimall.cart.dto.CartItemAddRequest(7L, 1)));
        verifyNoInteractions(cartItemMapper);
    }

    @Test
    /** 修改购物车数量不能超过扣除预留后的可售库存。 */
    void updateRejectsQuantityAboveAvailableStock() {
        CartItem item = new CartItem();
        item.setId(1L);
        item.setUserId(99L);
        item.setProductId(7L);
        item.setQuantity(1);
        when(cartItemMapper.selectOne(any())).thenReturn(item);
        when(productMapper.selectById(7L)).thenReturn(product(7L, 10, 8, 1));

        assertThrows(com.aimall.cart.exception.CartProductUnavailableException.class,
                () -> service.update(99L, 7L, new com.aimall.cart.dto.CartItemQuantityRequest(3)));
        verify(cartItemMapper, never()).updateById(any(CartItem.class));
    }

    private Product product(Long id, int stock, int reservedStock, int status) {
        Product product = new Product();
        product.setId(id);
        product.setSku("SKU-" + id);
        product.setName("测试商品");
        product.setPrice(new BigDecimal("10.00"));
        product.setStock(stock);
        product.setReservedStock(reservedStock);
        product.setStatus(status);
        return product;
    }
}
