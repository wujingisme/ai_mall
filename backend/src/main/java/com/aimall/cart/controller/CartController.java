package com.aimall.cart.controller;

import com.aimall.cart.dto.CartItemAddRequest;
import com.aimall.cart.dto.CartItemQuantityRequest;
import com.aimall.cart.service.CartService;
import com.aimall.cart.vo.CartResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/cart")
/** 购物车 HTTP 入口；所有操作都从 JWT 获取当前用户。 */
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) { this.cartService = cartService; }

    @GetMapping
    /** 查询当前用户购物车及可结算汇总金额。 */
    public CartResponse get(Authentication authentication) { return cartService.get(userId(authentication)); }

    @PostMapping("/items")
    /** 把商品加入当前用户购物车，数量会与已有数量累加。 */
    public CartResponse add(Authentication authentication, @Valid @RequestBody CartItemAddRequest request) {
        return cartService.add(userId(authentication), request);
    }

    @PutMapping("/items/{productId}")
    /** 将某件商品的数量替换为指定值。 */
    public CartResponse update(Authentication authentication, @PathVariable @Positive Long productId,
                               @Valid @RequestBody CartItemQuantityRequest request) {
        return cartService.update(userId(authentication), productId, request);
    }

    @DeleteMapping("/items/{productId}")
    /** 删除某件商品；重复删除保持幂等。 */
    public CartResponse remove(Authentication authentication, @PathVariable @Positive Long productId) {
        return cartService.remove(userId(authentication), productId);
    }

    @DeleteMapping("/items")
    /** 清空当前用户的全部购物车条目。 */
    public CartResponse clear(Authentication authentication) { return cartService.clear(userId(authentication)); }

    private Long userId(Authentication authentication) {
        // JWT 过滤器把已认证用户 ID 放入 principal，购物车归属绝不接受客户端传入的 userId。
        return Long.valueOf(authentication.getPrincipal().toString());
    }
}
