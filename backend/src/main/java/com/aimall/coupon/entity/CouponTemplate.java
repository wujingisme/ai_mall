package com.aimall.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("coupon_template")
public class CouponTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String couponType;
    private BigDecimal minimumSpend;
    private BigDecimal discountAmount;
    private Integer totalQuantity;
    private Integer issuedQuantity;
    private Integer perUserLimit;
    private String validityType;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Integer validDays;
    private Boolean shareEnabled;
    private String status;
    private LocalDateTime createdAt;
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
