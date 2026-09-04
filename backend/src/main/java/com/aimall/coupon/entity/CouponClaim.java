package com.aimall.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 分享领取审计实体；数据库唯一键保证同一分享同一用户最多一条记录。 */
@TableName("coupon_claim")
public class CouponClaim {
    /** 领取审计主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 被领取的分享凭证 ID。 */
    private Long shareId;
    /** 实际领取人的用户 ID。 */
    private Long claimantUserId;
    /** 因领取而新建的用户券 ID。 */
    private Long userCouponId;
    /** 领取时间。 */
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Long getShareId() { return shareId; }
    public Long getClaimantUserId() { return claimantUserId; }
    public Long getUserCouponId() { return userCouponId; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setShareId(Long value) { shareId = value; }
    public void setClaimantUserId(Long value) { claimantUserId = value; }
    public void setUserCouponId(Long value) { userCouponId = value; }
}
