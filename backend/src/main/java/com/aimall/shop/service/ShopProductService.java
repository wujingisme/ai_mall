package com.aimall.shop.service;

import com.aimall.common.exception.ProductNotFoundException;
import com.aimall.product.entity.Product;
import com.aimall.product.mapper.ProductMapper;
import com.aimall.shop.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
/** 消费端商品业务层，专门隔离“只可见已上架商品”的读取规则。 */
public class ShopProductService {
    private final ProductMapper productMapper;

    public ShopProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    /** 分页查询上架商品，并把内部 Product 转为最小展示字段。 */
    public ShopProductPageResponse list(int page, int pageSize, String keyword) {
        // 消费端在查询层强制限制为已上架，不能信任客户端自行传入 status。
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            query.like(Product::getName, keyword.trim());
        }
        query.orderByAsc(Product::getStock).orderByDesc(Product::getCreatedAt).orderByDesc(Product::getId);
        Page<Product> result = productMapper.selectPage(Page.of(page, pageSize), query);
        return new ShopProductPageResponse(
                result.getRecords().stream().map(this::toListItem).toList(),
                result.getCurrent(), result.getSize(), result.getTotal(), result.getPages());
    }

    /** 查询一个仍然上架的商品；下架/不存在统一返回商品不存在。 */
    public ShopProductDetailResponse get(Long id) {
        // 下架商品在消费者视角等同于不存在，防止通过猜测 ID 绕过上下架控制。
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, id)
                .eq(Product::getStatus, 1));
        if (product == null) throw new ProductNotFoundException(id);
        return new ShopProductDetailResponse(product.getId(), product.getName(), product.getPrice(),
                product.getImageUrl(), product.getDescription(), product.getStock() <= 0);
    }

    private ShopProductListItemResponse toListItem(Product product) {
        return new ShopProductListItemResponse(product.getId(), product.getName(), product.getPrice(),
                product.getImageUrl(), product.getStock() <= 0);
    }
}
