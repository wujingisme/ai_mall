package com.aimall.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 分享凭证实体；token 只保存哈希，claimedCount/maxClaims 控制领取次数。 */
@TableName("coupon_share")
public class CouponShare {
    /** 分享主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 来源模板 ID。 */
    private Long templateId;
    /** 创建分享的用户 ID。 */
    private Long creatorUserId;
    /** 创建分享时使用的原始用户券 ID。 */
    private Long creatorUserCouponId;
    /** 随机分享 token 的 SHA-256 摘要。 */
    private String tokenHash;
    /** 最大领取次数，首版固定为 1。 */
    private Integer maxClaims;
    /** 已领取次数。 */
    private Integer claimedCount;
    /** ACTIVE 或 REVOKED。 */
    private String status;
    /** 分享过期时间。 */
    private LocalDateTime expiresAt;
    /** 分享创建时间。 */
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Long getTemplateId() { return templateId; }
    public Long getCreatorUserId() { return creatorUserId; }
    public Long getCreatorUserCouponId() { return creatorUserCouponId; }
    public String getTokenHash() { return tokenHash; }
    public Integer getMaxClaims() { return maxClaims; }
    public Integer getClaimedCount() { return claimedCount; }
    public String getStatus() { return status; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setTemplateId(Long value) { templateId = value; }
    public void setCreatorUserId(Long value) { creatorUserId = value; }
    public void setCreatorUserCouponId(Long value) { creatorUserCouponId = value; }
    public void setTokenHash(String value) { tokenHash = value; }
    public void setMaxClaims(Integer value) { maxClaims = value; }
    public void setClaimedCount(Integer value) { claimedCount = value; }
    public void setStatus(String value) { status = value; }
    public void setExpiresAt(LocalDateTime value) { expiresAt = value; }
}
