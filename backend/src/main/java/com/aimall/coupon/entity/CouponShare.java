package com.aimall.coupon.entity;
import com.baomidou.mybatisplus.annotation.*; import java.time.LocalDateTime;
@TableName("coupon_share") public class CouponShare {
 @TableId(type=IdType.AUTO) private Long id; private Long templateId,creatorUserId,creatorUserCouponId; private String tokenHash; private Integer maxClaims,claimedCount; private String status; private LocalDateTime expiresAt,createdAt;
 public Long getId(){return id;} public Long getTemplateId(){return templateId;} public Long getCreatorUserId(){return creatorUserId;} public Long getCreatorUserCouponId(){return creatorUserCouponId;} public String getTokenHash(){return tokenHash;} public Integer getMaxClaims(){return maxClaims;} public Integer getClaimedCount(){return claimedCount;} public String getStatus(){return status;} public LocalDateTime getExpiresAt(){return expiresAt;} public LocalDateTime getCreatedAt(){return createdAt;}
 public void setTemplateId(Long v){templateId=v;} public void setCreatorUserId(Long v){creatorUserId=v;} public void setCreatorUserCouponId(Long v){creatorUserCouponId=v;} public void setTokenHash(String v){tokenHash=v;} public void setMaxClaims(Integer v){maxClaims=v;} public void setClaimedCount(Integer v){claimedCount=v;} public void setStatus(String v){status=v;} public void setExpiresAt(LocalDateTime v){expiresAt=v;}
}
