package com.aimall.product.service;

import com.aimall.common.exception.*;
import com.aimall.product.dto.ProductWriteRequest;
import com.aimall.product.entity.Product;
import com.aimall.product.mapper.ProductMapper;
import com.aimall.product.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductService {
    private final ProductMapper productMapper;
    public ProductService(ProductMapper productMapper) { this.productMapper = productMapper; }

    public ProductPageResponse list(int page, int pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            query.and(w -> w.like(Product::getName, value).or().like(Product::getSku, value));
        }
        query.eq(status != null, Product::getStatus, status)
                .orderByDesc(Product::getCreatedAt).orderByDesc(Product::getId);
        Page<Product> result = productMapper.selectPage(Page.of(page, pageSize), query);
        return new ProductPageResponse(result.getRecords().stream().map(this::toListItem).toList(),
                result.getCurrent(), result.getSize(), result.getTotal(), result.getPages());
    }

    public ProductDetailResponse get(Long id) { return toDetail(requireProduct(id)); }

    @Transactional
    public ProductDetailResponse create(ProductWriteRequest request) {
        ensureSkuAvailable(request.sku().trim(), null);
        Product product = new Product();
        apply(product, request);
        productMapper.insert(product);
        return toDetail(requireProduct(product.getId()));
    }

    @Transactional
    public ProductDetailResponse update(Long id, ProductWriteRequest request) {
        Product product = requireProduct(id);
        ensureSkuAvailable(request.sku().trim(), id);
        apply(product, request);
        // PUT 接收完整商品表单：所有可编辑字段都必须覆盖，null 表示主动清空可选字段。
        // 显式 set 避免 updateById 默认忽略 null，从而保证图片和描述可以被清除。
        productMapper.update(null, Wrappers.lambdaUpdate(Product.class)
                .eq(Product::getId, id)
                .set(Product::getSku, product.getSku())
                .set(Product::getName, product.getName())
                .set(Product::getPrice, product.getPrice())
                .set(Product::getStock, product.getStock())
                .set(Product::getStatus, product.getStatus())
                .set(Product::getImageUrl, product.getImageUrl())
                .set(Product::getDescription, product.getDescription()));
        return toDetail(requireProduct(id));
    }

    @Transactional
    public void delete(Long id) {
        requireProduct(id);
        productMapper.deleteById(id);
    }

    private Product requireProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) throw new ProductNotFoundException(id);
        return product;
    }

    private void ensureSkuAvailable(String sku, Long excludedId) {
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<Product>().eq(Product::getSku, sku);
        if (excludedId != null) query.ne(Product::getId, excludedId);
        if (productMapper.selectCount(query) > 0) throw new SkuConflictException(sku);
    }

    private void apply(Product p, ProductWriteRequest r) {
        p.setSku(r.sku().trim()); p.setName(r.name().trim()); p.setPrice(r.price());
        p.setStock(r.stock()); p.setStatus(r.status()); p.setImageUrl(trimToNull(r.imageUrl()));
        p.setDescription(trimToNull(r.description()));
    }

    private String trimToNull(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
    private ProductListItemResponse toListItem(Product p) {
        return new ProductListItemResponse(p.getId(), p.getSku(), p.getName(), p.getPrice(), p.getStock(),
                p.getStatus(), p.getImageUrl(), p.getCreatedAt(), p.getUpdatedAt());
    }
    private ProductDetailResponse toDetail(Product p) {
        return new ProductDetailResponse(p.getId(), p.getSku(), p.getName(), p.getPrice(), p.getStock(),
                p.getStatus(), p.getImageUrl(), p.getDescription(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
