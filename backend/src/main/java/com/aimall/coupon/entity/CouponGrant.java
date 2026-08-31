package com.aimall.coupon.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("coupon_grant")
public class CouponGrant {
    @TableId(type = IdType.AUTO) private Long id;
    private Long templateId;
    private Long targetUserId;
    private Long operatorUserId;
    private Integer requestedQuantity;
    private Integer successQuantity;
    private String reason;
    private String idempotencyKey;
    private String status;
    private LocalDateTime createdAt;
    public Long getId() { return id; }
    public Long getTemplateId() { return templateId; }
    public Long getTargetUserId() { return targetUserId; }
    public Long getOperatorUserId() { return operatorUserId; }
    public Integer getRequestedQuantity() { return requestedQuantity; }
    public Integer getSuccessQuantity() { return successQuantity; }
    public String getReason() { return reason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setTemplateId(Long value) { templateId = value; }
    public void setTargetUserId(Long value) { targetUserId = value; }
    public void setOperatorUserId(Long value) { operatorUserId = value; }
    public void setRequestedQuantity(Integer value) { requestedQuantity = value; }
    public void setSuccessQuantity(Integer value) { successQuantity = value; }
    public void setReason(String value) { reason = value; }
    public void setIdempotencyKey(String value) { idempotencyKey = value; }
    public void setStatus(String value) { status = value; }
}
