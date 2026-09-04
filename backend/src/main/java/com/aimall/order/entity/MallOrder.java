package com.aimall.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表实体，对应数据库中的 {@code mall_order}。
 *
 * <p>订单保存下单时的金额、取货点和状态。取货码哈希只供后端核销，不能通过 VO 返回给用户。</p>
 */
@TableName("mall_order")
public class MallOrder {
    /** 数据库自增主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 对外展示的业务订单号。 */
    private String orderNo;
    /** 下单用户 ID；所有“我的订单”查询都必须带上它。 */
    private Long userId;
    /** PENDING_PICKUP、PICKED_UP 或 CANCELLED。 */
    private String status;
    /** 固定取货点名称快照。 */
    private String pickupLocationName;
    /** 固定取货点地址快照。 */
    private String pickupLocationAddress;
    /** 取货码哈希；明文只在创建订单响应中返回一次。 */
    private String pickupCodeHash;
    /** 后端根据商品价格计算的订单总金额。 */
    private BigDecimal totalAmount;
    /** 商品总件数快照，便于列表展示。 */
    private Integer itemQuantity;
    /** 创建订单时的客户端幂等键，第二阶段启用。 */
    private String idempotencyKey;
    /** 取消时间；未取消时为空。 */
    private LocalDateTime cancelledAt;
    /** 完成取货时间；未取货时为空。 */
    private LocalDateTime pickedUpAt;
    /** 下单时间。 */
    private LocalDateTime createdAt;
    /** 最近修改时间。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String value) { orderNo = value; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { userId = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getPickupLocationName() { return pickupLocationName; }
    public void setPickupLocationName(String value) { pickupLocationName = value; }
    public String getPickupLocationAddress() { return pickupLocationAddress; }
    public void setPickupLocationAddress(String value) { pickupLocationAddress = value; }
    public String getPickupCodeHash() { return pickupCodeHash; }
    public void setPickupCodeHash(String value) { pickupCodeHash = value; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal value) { totalAmount = value; }
    public Integer getItemQuantity() { return itemQuantity; }
    public void setItemQuantity(Integer value) { itemQuantity = value; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String value) { idempotencyKey = value; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime value) { cancelledAt = value; }
    public LocalDateTime getPickedUpAt() { return pickedUpAt; }
    public void setPickedUpAt(LocalDateTime value) { pickedUpAt = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { updatedAt = value; }
}
