package com.aimall.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("coupon_template")
/** 优惠券模板实体：保存一批券的规则、库存、有效期和生命周期状态。 */
public class CouponTemplate {
    /** 模板主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 后台展示名称。 */
    private String name;
    /** 首版固定为 FIXED_AMOUNT，表示满减券。 */
    private String couponType;
    /** 使用门槛，例如满 100。 */
    private BigDecimal minimumSpend;
    /** 满足门槛后减免的金额，例如减 20。 */
    private BigDecimal discountAmount;
    /** 允许发行的总张数。 */
    private Integer totalQuantity;
    /** 已被人工发放或分享领取预留的张数。 */
    private Integer issuedQuantity;
    /** 同一用户最多拥有的张数。 */
    private Integer perUserLimit;
    /** FIXED_RANGE 或 DAYS_AFTER_RECEIPT。 */
    private String validityType;
    /** 固定有效期开始时间；领取后有效模式为空。 */
    private LocalDateTime validFrom;
    /** 固定有效期结束时间；领取后有效模式为空。 */
    private LocalDateTime validUntil;
    /** 领取后有效天数；固定范围模式为空。 */
    private Integer validDays;
    /** 是否允许用户创建分享凭证。 */
    private Boolean shareEnabled;
    /** DRAFT、ACTIVE 或 DISABLED。 */
    private String status;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 最近修改时间。 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCouponType() { return couponType; }
    public void setCouponType(String couponType) { this.couponType = couponType; }
    public BigDecimal getMinimumSpend() { return minimumSpend; }
    public void setMinimumSpend(BigDecimal minimumSpend) { this.minimumSpend = minimumSpend; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
    public Integer getIssuedQuantity() { return issuedQuantity; }
    public void setIssuedQuantity(Integer issuedQuantity) { this.issuedQuantity = issuedQuantity; }
    public Integer getPerUserLimit() { return perUserLimit; }
    public void setPerUserLimit(Integer perUserLimit) { this.perUserLimit = perUserLimit; }
    public String getValidityType() { return validityType; }
    public void setValidityType(String validityType) { this.validityType = validityType; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }
    public Integer getValidDays() { return validDays; }
    public void setValidDays(Integer validDays) { this.validDays = validDays; }
    public Boolean getShareEnabled() { return shareEnabled; }
    public void setShareEnabled(Boolean shareEnabled) { this.shareEnabled = shareEnabled; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
