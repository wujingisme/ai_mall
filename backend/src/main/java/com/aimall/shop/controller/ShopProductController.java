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
/** 消费端只读商品接口；不暴露后台商品编辑能力。 */
public class ShopProductController {
    private final ShopProductService shopProductService;

    public ShopProductController(ShopProductService shopProductService) {
        this.shopProductService = shopProductService;
    }

    @GetMapping
    /** 查询已上架商品列表，具体 status 过滤由 Service 强制执行。 */
    public ShopProductPageResponse list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) @Size(max = 200) String keyword) {
        return shopProductService.list(page, pageSize, keyword);
    }

    @GetMapping("/{id}")
    /** 查询消费端商品详情；下架商品在消费者视角等同于不存在。 */
    public ShopProductDetailResponse get(@PathVariable @Min(1) Long id) {
        return shopProductService.get(id);
    }
}
