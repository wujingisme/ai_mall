package com.aimall.product.service;

import com.aimall.common.exception.ProductStockConflictException;
import com.aimall.product.dto.ProductWriteRequest;
import com.aimall.product.entity.Product;
import com.aimall.product.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/** 商品 Service 单元测试：重点验证 PUT 完整替换和 null 清空语义。 */
class ProductServiceTest {
    private final ProductMapper mapper = mock(ProductMapper.class);
    private final ProductService service = new ProductService(mapper);

    @BeforeAll
    /** 初始化 MyBatis 的实体元数据，使 LambdaUpdateWrapper 能解析 Product 字段。 */
    static void initializeMybatisMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "product-test"), Product.class);
    }

    @Test
    /** 空图片和空描述必须作为 SQL NULL 写入，而不能被 updateById 忽略。 */
    void fullUpdateIncludesNullOptionalFields() {
        Product product = product(1L, "OLD", "旧商品", "https://example.com/old.jpg", "旧描述");
        when(mapper.selectForUpdate(1L)).thenReturn(product);
        when(mapper.selectById(1L)).thenReturn(product);
        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        var response = service.update(1L, new ProductWriteRequest(
                " NEW ", " 新商品 ", new BigDecimal("19.90"), 8, 1, "   ", ""));

        assertEquals("NEW", response.sku());
        assertEquals("新商品", response.name());
        assertNull(response.imageUrl());
        assertNull(response.description());

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<LambdaUpdateWrapper> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(mapper).update(isNull(), captor.capture());
        String sqlSet = captor.getValue().getSqlSet();
        assertTrue(sqlSet.contains("image_url="), "完整更新必须包含 image_url");
        assertTrue(sqlSet.contains("description="), "完整更新必须包含 description");
        assertEquals(7, captor.getValue().getParamNameValuePairs().size(), "全部七个可编辑字段都应参与更新");
        assertEquals(2, captor.getValue().getParamNameValuePairs().values().stream().filter(value -> value == null).count());
    }

    @Test
    /** 总库存不能改到已预留库存以下，否则取消或核销订单时无法维持库存不变量。 */
    void updateRejectsStockBelowReservedStock() {
        Product product = product(1L, "SKU-1", "商品", "https://example.com/old.jpg", "旧描述");
        product.setStock(5);
        product.setReservedStock(3);
        // update 方法先锁定最新商品行，再读取 reserved_stock；测试也要模拟这次加锁读取。
        when(mapper.selectForUpdate(1L)).thenReturn(product);
        when(mapper.selectById(1L)).thenReturn(product);
        when(mapper.selectCount(any())).thenReturn(0L);

        assertThrows(ProductStockConflictException.class, () -> service.update(1L,
                new ProductWriteRequest("SKU-1", "商品", new BigDecimal("9.90"), 2, 1, null, null)));
        verify(mapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    /** 有待取货订单预留库存时不能删除商品，否则订单取消时将无法找到商品释放库存。 */
    void deleteRejectsProductWithReservedStock() {
        Product product = product(1L, "SKU-1", "商品", null, null);
        product.setReservedStock(1);
        when(mapper.selectForUpdate(1L)).thenReturn(product);

        assertThrows(ProductStockConflictException.class, () -> service.delete(1L));
        verify(mapper, never()).deleteById(1L);
    }

    private Product product(Long id, String sku, String name, String imageUrl, String description) {
        // 构造一次已有数据库记录，模拟编辑前读取到的旧商品。
        Product product = new Product();
        product.setId(id);
        product.setSku(sku);
        product.setName(name);
        product.setPrice(new BigDecimal("9.90"));
        product.setStock(2);
        product.setReservedStock(0);
        product.setStatus(1);
        product.setImageUrl(imageUrl);
        product.setDescription(description);
        return product;
    }
}
