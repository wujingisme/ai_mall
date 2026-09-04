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
/** 商品后台业务层：负责校验 SKU、规范化字段、调用 Mapper 并组装返回 VO。 */
public class ProductService {
    private final ProductMapper productMapper;
    public ProductService(ProductMapper productMapper) { this.productMapper = productMapper; }

    /** 分页查询后台商品，可按名称/SKU 关键字和上下架状态过滤。 */
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

    /** 通过 ID 查询商品；找不到时转换为稳定的业务异常。 */
    public ProductDetailResponse get(Long id) { return toDetail(requireProduct(id)); }

    @Transactional
    /** 创建商品，并在插入前主动检查 SKU；数据库唯一索引仍是并发兜底。 */
    public ProductDetailResponse create(ProductWriteRequest request) {
        ensureSkuAvailable(request.sku().trim(), null);
        Product product = new Product();
        apply(product, request);
        productMapper.insert(product);
        return toDetail(requireProduct(product.getId()));
    }

    @Transactional
    /** 完整替换商品；显式 set 允许 PUT 中的 null 清除旧图片和描述。 */
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
    /** 删除前先查询，确保不存在时返回 404 而不是静默成功。 */
    public void delete(Long id) {
        requireProduct(id);
        productMapper.deleteById(id);
    }

    private Product requireProduct(Long id) {
        // 集中封装“按 ID 查询并保证存在”，避免每个公开方法重复写 null 判断。
        Product product = productMapper.selectById(id);
        if (product == null) throw new ProductNotFoundException(id);
        return product;
    }

    private void ensureSkuAvailable(String sku, Long excludedId) {
        // 编辑时排除当前商品自身，否则不修改 SKU 的正常 PUT 会被误判为冲突。
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<Product>().eq(Product::getSku, sku);
        if (excludedId != null) query.ne(Product::getId, excludedId);
        if (productMapper.selectCount(query) > 0) throw new SkuConflictException(sku);
    }

    private void apply(Product p, ProductWriteRequest r) {
        // 统一在 Service 层 trim 文本并把空字符串转成 null，保持数据库语义稳定。
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
