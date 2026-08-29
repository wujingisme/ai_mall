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
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) { this.cartService = cartService; }

    @GetMapping
    public CartResponse get(Authentication authentication) { return cartService.get(userId(authentication)); }

    @PostMapping("/items")
    public CartResponse add(Authentication authentication, @Valid @RequestBody CartItemAddRequest request) {
        return cartService.add(userId(authentication), request);
    }

    @PutMapping("/items/{productId}")
    public CartResponse update(Authentication authentication, @PathVariable @Positive Long productId,
                               @Valid @RequestBody CartItemQuantityRequest request) {
        return cartService.update(userId(authentication), productId, request);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse remove(Authentication authentication, @PathVariable @Positive Long productId) {
        return cartService.remove(userId(authentication), productId);
    }

    @DeleteMapping("/items")
    public CartResponse clear(Authentication authentication) { return cartService.clear(userId(authentication)); }

    private Long userId(Authentication authentication) {
        // JWT 过滤器把已认证用户 ID 放入 principal，购物车归属绝不接受客户端传入的 userId。
        return Long.valueOf(authentication.getPrincipal().toString());
    }
}
