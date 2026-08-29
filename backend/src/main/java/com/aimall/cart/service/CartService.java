package com.aimall.cart.service;

import com.aimall.cart.dto.CartItemAddRequest;
import com.aimall.cart.dto.CartItemQuantityRequest;
import com.aimall.cart.entity.CartItem;
import com.aimall.cart.exception.CartItemNotFoundException;
import com.aimall.cart.exception.CartProductUnavailableException;
import com.aimall.cart.mapper.CartItemMapper;
import com.aimall.cart.vo.CartItemResponse;
import com.aimall.cart.vo.CartResponse;
import com.aimall.common.exception.ProductNotFoundException;
import com.aimall.product.entity.Product;
import com.aimall.product.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CartService {
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;

    public CartService(CartItemMapper cartItemMapper, ProductMapper productMapper) {
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
    }

    public CartResponse get(Long userId) {
        List<CartItem> cartItems = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId).orderByDesc(CartItem::getUpdatedAt));
        if (cartItems.isEmpty()) return new CartResponse(List.of(), 0, BigDecimal.ZERO);

        Map<Long, Product> products = productMapper.selectBatchIds(
                        cartItems.stream().map(CartItem::getProductId).toList()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<CartItemResponse> items = cartItems.stream()
                .map(item -> toResponse(item, products.get(item.getProductId())))
                .toList();
        int totalQuantity = items.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal totalAmount = items.stream().filter(CartItemResponse::available)
                .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(items, totalQuantity, totalAmount);
    }

    @Transactional
    public CartResponse add(Long userId, CartItemAddRequest request) {
        Product product = requirePurchasableProduct(request.productId());
        CartItem item = find(userId, request.productId());
        int quantity = request.quantity() + (item == null ? 0 : item.getQuantity());
        validateQuantity(quantity, product);
        if (item == null) {
            item = new CartItem();
            item.setUserId(userId);
            item.setProductId(request.productId());
            item.setQuantity(quantity);
            cartItemMapper.insert(item);
        } else {
            item.setQuantity(quantity);
            cartItemMapper.updateById(item);
        }
        return get(userId);
    }

    @Transactional
    public CartResponse update(Long userId, Long productId, CartItemQuantityRequest request) {
        CartItem item = find(userId, productId);
        if (item == null) throw new CartItemNotFoundException(productId);
        Product product = requirePurchasableProduct(productId);
        validateQuantity(request.quantity(), product);
        item.setQuantity(request.quantity());
        cartItemMapper.updateById(item);
        return get(userId);
    }

    @Transactional
    public CartResponse remove(Long userId, Long productId) {
        // 删除操作保持幂等，重复点击或网络重试不会产生额外错误。
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId).eq(CartItem::getProductId, productId));
        return get(userId);
    }

    @Transactional
    public CartResponse clear(Long userId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
        return new CartResponse(List.of(), 0, BigDecimal.ZERO);
    }

    private CartItem find(Long userId, Long productId) {
        return cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId).eq(CartItem::getProductId, productId));
    }

    private Product requirePurchasableProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) throw new ProductNotFoundException(productId);
        if (product.getStatus() == null || product.getStatus() != 1 || product.getStock() == null || product.getStock() <= 0) {
            throw new CartProductUnavailableException("商品已下架或售罄");
        }
        return product;
    }

    private void validateQuantity(int quantity, Product product) {
        if (quantity > 99) throw new CartProductUnavailableException("单件商品最多加入 99 件");
        if (quantity > product.getStock()) throw new CartProductUnavailableException("商品库存不足，仅剩 " + product.getStock() + " 件");
    }

    private CartItemResponse toResponse(CartItem item, Product product) {
        // 商品即使后来下架也保留在购物车中展示，但不计入可结算金额。
        if (product == null) return new CartItemResponse(item.getProductId(), "商品已不存在", BigDecimal.ZERO,
                null, item.getQuantity(), 0, false);
        int stock = product.getStock() == null ? 0 : product.getStock();
        boolean available = product.getStatus() != null && product.getStatus() == 1 && stock >= item.getQuantity();
        return new CartItemResponse(product.getId(), product.getName(), product.getPrice(), product.getImageUrl(),
                item.getQuantity(), stock, available);
    }
}
