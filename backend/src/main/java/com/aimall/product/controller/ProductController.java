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
public class ProductController {
    private final ProductService productService;
    public ProductController(ProductService productService) { this.productService = productService; }

    @GetMapping
    ProductPageResponse list(@RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status) {
        return productService.list(page, pageSize, keyword, status);
    }

    @GetMapping("/{id}")
    ProductDetailResponse get(@PathVariable @Min(1) Long id) { return productService.get(id); }

    @PostMapping
    ResponseEntity<ProductDetailResponse> create(@Valid @RequestBody ProductWriteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @PutMapping("/{id}")
    ProductDetailResponse update(@PathVariable @Min(1) Long id, @Valid @RequestBody ProductWriteRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable @Min(1) Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
