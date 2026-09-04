package com.aimall.cart.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("cart_item")
/** 购物车实体，对应一个用户和一个商品的数量记录。 */
public class CartItem {
    /** 购物车行主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 购物车归属用户；所有查询必须带上此条件。 */
    private Long userId;
    /** 商品主键；商品删除时数据库外键会级联删除购物车行。 */
    private Long productId;
    /** 用户想购买的数量，数据库限制为 1 到 99。 */
    private Integer quantity;
    /** 首次加入时间。 */
    private LocalDateTime createdAt;
    /** 最近修改时间，用于购物车排序。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
