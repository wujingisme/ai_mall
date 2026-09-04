package com.aimall.product.controller;

import com.aimall.product.dto.ProductWriteRequest;
import com.aimall.product.service.ProductService;
import com.aimall.product.vo.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/products")
/** 后台商品 CRUD 的 HTTP 入口；角色权限在 SecurityConfig 中统一限制。 */
public class ProductController {
    private final ProductService productService;
    public ProductController(ProductService productService) { this.productService = productService; }

    @GetMapping
    /** 分页查询商品，关键字同时匹配名称和 SKU。 */
    ProductPageResponse list(@RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status) {
        return productService.list(page, pageSize, keyword, status);
    }

    @GetMapping("/{id}")
    /** 查询后台商品详情，包括库存、状态和 SKU。 */
    ProductDetailResponse get(@PathVariable @Min(1) Long id) { return productService.get(id); }

    @PostMapping
    /** 创建商品；成功使用 201，SKU 冲突由全局异常处理器返回 409。 */
    ResponseEntity<ProductDetailResponse> create(@Valid @RequestBody ProductWriteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @PutMapping("/{id}")
    /** 完整替换商品的可编辑字段，空图片/描述可以清除旧值。 */
    ProductDetailResponse update(@PathVariable @Min(1) Long id, @Valid @RequestBody ProductWriteRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    /** 删除商品；不存在时 Service 抛出统一的商品不存在异常。 */
    ResponseEntity<Void> delete(@PathVariable @Min(1) Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
