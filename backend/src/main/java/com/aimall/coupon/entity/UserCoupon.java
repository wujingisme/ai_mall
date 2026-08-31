package com.aimall.coupon.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("user_coupon")
public class UserCoupon {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private Long templateId;
    private Long grantId;
    private Long shareId;
    private String source;
    private String name;
    private String couponType;
    private BigDecimal minimumSpend;
    private BigDecimal discountAmount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private String status;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getTemplateId() { return templateId; }
    public Long getGrantId() { return grantId; }
    public Long getShareId() { return shareId; }
    public String getSource() { return source; }
    public String getName() { return name; }
    public String getCouponType() { return couponType; }
    public BigDecimal getMinimumSpend() { return minimumSpend; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public LocalDateTime getValidUntil() { return validUntil; }
    public String getStatus() { return status; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUserId(Long value) { userId = value; }
    public void setTemplateId(Long value) { templateId = value; }
    public void setGrantId(Long value) { grantId = value; }
    public void setShareId(Long value) { shareId = value; }
    public void setSource(String value) { source = value; }
    public void setName(String value) { name = value; }
    public void setCouponType(String value) { couponType = value; }
    public void setMinimumSpend(BigDecimal value) { minimumSpend = value; }
    public void setDiscountAmount(BigDecimal value) { discountAmount = value; }
    public void setValidFrom(LocalDateTime value) { validFrom = value; }
    public void setValidUntil(LocalDateTime value) { validUntil = value; }
    public void setStatus(String value) { status = value; }
}
