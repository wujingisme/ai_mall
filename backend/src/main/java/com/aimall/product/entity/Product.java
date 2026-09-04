package com.aimall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("product")
/** 商品数据库实体；消费端通过另一组 VO 做字段裁剪。 */
public class Product {
    /** 自增商品主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 商家内部商品编码，必须唯一。 */
    private String sku;
    /** 商品展示名称。 */
    private String name;
    /** 使用 BigDecimal 保存金额，避免 double 浮点误差。 */
    private BigDecimal price;
    /** 当前库存数量。 */
    private Integer stock;
    /** 0 下架，1 上架。 */
    private Integer status;
    /** 主图 URL，可为空。 */
    private String imageUrl;
    /** 详情描述，可为空。 */
    private String description;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 最近修改时间。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
