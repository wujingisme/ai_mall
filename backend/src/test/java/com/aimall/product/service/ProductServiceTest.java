package com.aimall.product.service;

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

class ProductServiceTest {
    private final ProductMapper mapper = mock(ProductMapper.class);
    private final ProductService service = new ProductService(mapper);

    @BeforeAll
    static void initializeMybatisMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "product-test"), Product.class);
    }

    @Test
    void fullUpdateIncludesNullOptionalFields() {
        Product product = product(1L, "OLD", "旧商品", "https://example.com/old.jpg", "旧描述");
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

    private Product product(Long id, String sku, String name, String imageUrl, String description) {
        Product product = new Product();
        product.setId(id);
        product.setSku(sku);
        product.setName(name);
        product.setPrice(new BigDecimal("9.90"));
        product.setStock(2);
        product.setStatus(1);
        product.setImageUrl(imageUrl);
        product.setDescription(description);
        return product;
    }
}
