package com.aimall.coupon.entity;
import com.baomidou.mybatisplus.annotation.*; import java.time.LocalDateTime;
@TableName("coupon_claim") public class CouponClaim {
 @TableId(type=IdType.AUTO) private Long id; private Long shareId,claimantUserId,userCouponId; private LocalDateTime createdAt;
 public Long getId(){return id;} public Long getShareId(){return shareId;} public Long getClaimantUserId(){return claimantUserId;} public Long getUserCouponId(){return userCouponId;} public LocalDateTime getCreatedAt(){return createdAt;}
 public void setShareId(Long v){shareId=v;} public void setClaimantUserId(Long v){claimantUserId=v;} public void setUserCouponId(Long v){userCouponId=v;}
}
