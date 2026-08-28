package com.aimall.shop.controller;

import com.aimall.shop.service.ShopProductService;
import com.aimall.shop.vo.ShopProductDetailResponse;
import com.aimall.shop.vo.ShopProductPageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/shop/products")
public class ShopProductController {
    private final ShopProductService shopProductService;

    public ShopProductController(ShopProductService shopProductService) {
        this.shopProductService = shopProductService;
    }

    @GetMapping
    public ShopProductPageResponse list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) @Size(max = 200) String keyword) {
        return shopProductService.list(page, pageSize, keyword);
    }

    @GetMapping("/{id}")
    public ShopProductDetailResponse get(@PathVariable @Min(1) Long id) {
        return shopProductService.get(id);
    }
}
