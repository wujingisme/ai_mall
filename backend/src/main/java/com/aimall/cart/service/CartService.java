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
/**
 * 购物车业务层。
 *
 * <p>购物车表只保存用户、商品和数量；名称、价格、库存等展示信息每次从商品表读取，
 * 所以商品下架或库存变化后，购物车能显示最新可用性。</p>
 */
public class CartService {
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;

    public CartService(CartItemMapper cartItemMapper, ProductMapper productMapper) {
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
    }

    /** 查询当前用户购物车，并计算总数量和仍可购买商品的金额。 */
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
    /** 添加商品并与已有数量相加；商品必须存在、上架且可售库存充足。 */
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
    /** 替换某个商品的购买数量，并再次检查商品是否仍有足够可售库存。 */
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
    /** 删除当前用户的指定商品，数据库删除 0 行也视为成功。 */
    public CartResponse remove(Long userId, Long productId) {
        // 删除操作保持幂等，重复点击或网络重试不会产生额外错误。
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId).eq(CartItem::getProductId, productId));
        return get(userId);
    }

    @Transactional
    /** 删除当前用户全部购物车条目，并返回空汇总。 */
    public CartResponse clear(Long userId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
        return new CartResponse(List.of(), 0, BigDecimal.ZERO);
    }

    private CartItem find(Long userId, Long productId) {
        // 用户 ID 和商品 ID 同时作为条件，防止读到其他用户的同款商品。
        return cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId).eq(CartItem::getProductId, productId));
    }

    private Product requirePurchasableProduct(Long productId) {
        // 写操作必须以商品表为准，不能相信前端缓存的价格、状态或库存。
        Product product = productMapper.selectById(productId);
        if (product == null) throw new ProductNotFoundException(productId);
        if (product.getStatus() == null || product.getStatus() != 1 || availableStock(product) <= 0) {
            throw new CartProductUnavailableException("商品已下架或售罄");
        }
        return product;
    }

    private void validateQuantity(int quantity, Product product) {
        // 数据库还有 1-99 的 CHECK；这里提前给出更友好的业务错误消息。
        if (quantity > 99) throw new CartProductUnavailableException("单件商品最多加入 99 件");
        int stock = availableStock(product);
        if (quantity > stock) throw new CartProductUnavailableException("商品库存不足，仅剩 " + stock + " 件");
    }

    private CartItemResponse toResponse(CartItem item, Product product) {
        // 商品即使后来下架也保留在购物车中展示，但不计入可结算金额。
        if (product == null) return new CartItemResponse(item.getProductId(), "商品已不存在", BigDecimal.ZERO,
                null, item.getQuantity(), 0, false);
        // 对消费者返回的 stock 保持原字段兼容，但它现在代表“当前可售库存”，不是总库存。
        int stock = availableStock(product);
        boolean available = product.getStatus() != null && product.getStatus() == 1 && stock >= item.getQuantity();
        return new CartItemResponse(product.getId(), product.getName(), product.getPrice(), product.getImageUrl(),
                item.getQuantity(), stock, available);
    }

    /**
     * 计算购物车可用的库存数量。
     *
     * <p>订单预留会减少可售量，但不会立即减少 stock 总量；统一在这里计算，
     * 避免“加入购物车”和“购物车展示”使用不同库存口径。</p>
     */
    private int availableStock(Product product) {
        int stock = product.getStock() == null ? 0 : product.getStock();
        int reserved = product.getReservedStock() == null ? 0 : product.getReservedStock();
        return Math.max(stock - reserved, 0);
    }
}
