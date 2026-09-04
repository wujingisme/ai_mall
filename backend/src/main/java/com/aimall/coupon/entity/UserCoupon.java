package com.aimall.coupon.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("user_coupon")
/** 用户优惠券实例实体；保存发放当时的规则快照，模板后续变化不会篡改用户权益。 */
public class UserCoupon {
    /** 用户券实例主键。 */
    @TableId(type = IdType.AUTO) private Long id;
    /** 当前拥有这张券的用户。 */
    private Long userId;
    /** 来源模板 ID。 */
    private Long templateId;
    /** 人工发券审计 ID；分享领取时为空。 */
    private Long grantId;
    /** 分享凭证 ID；人工发券时为空。 */
    private Long shareId;
    /** MANUAL 或 SHARE。 */
    private String source;
    /** 发放时的名称快照。 */
    private String name;
    /** 发放时的券类型快照。 */
    private String couponType;
    /** 发放时的门槛快照。 */
    private BigDecimal minimumSpend;
    /** 发放时的优惠金额快照。 */
    private BigDecimal discountAmount;
    /** 这张券实际生效的开始时间。 */
    private LocalDateTime validFrom;
    /** 这张券实际失效的结束时间。 */
    private LocalDateTime validUntil;
    /** UNUSED 或 USED；EXPIRED 根据时间实时派生。 */
    private String status;
    /** 使用时间；未使用时为空。 */
    private LocalDateTime usedAt;
    /** 创建/领取时间。 */
    private LocalDateTime createdAt;
    /** 最近修改时间。 */
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
