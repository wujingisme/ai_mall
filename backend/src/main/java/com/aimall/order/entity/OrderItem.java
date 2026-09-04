package com.aimall.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订单商品明细实体；字段保存下单时的商品快照，不依赖商品当前名称或价格。 */
@TableName("order_item")
public class OrderItem {
    /** 明细主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属订单主键。 */
    private Long orderId;
    /** 原商品 ID，商品删除后可以为空但历史快照仍保留。 */
    private Long productId;
    /** 下单时 SKU 快照。 */
    private String sku;
    /** 下单时商品名称快照。 */
    private String productName;
    /** 下单时单价快照。 */
    private BigDecimal unitPrice;
    /** 下单数量。 */
    private Integer quantity;
    /** 单行金额快照，等于 unitPrice * quantity。 */
    private BigDecimal lineAmount;
    /** 明细创建时间。 */
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { orderId = value; }
    public Long getProductId() { return productId; }
    public void setProductId(Long value) { productId = value; }
    public String getSku() { return sku; }
    public void setSku(String value) { sku = value; }
    public String getProductName() { return productName; }
    public void setProductName(String value) { productName = value; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal value) { unitPrice = value; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer value) { quantity = value; }
    public BigDecimal getLineAmount() { return lineAmount; }
    public void setLineAmount(BigDecimal value) { lineAmount = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
}
