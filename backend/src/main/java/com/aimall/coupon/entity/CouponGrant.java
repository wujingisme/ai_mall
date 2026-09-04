package com.aimall.coupon.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("coupon_grant")
/** 人工发券审计实体：记录谁在何时因何原因给哪个用户发了多少张券。 */
public class CouponGrant {
    /** 发放审计主键。 */
    @TableId(type = IdType.AUTO) private Long id;
    /** 被使用的优惠券模板。 */
    private Long templateId;
    /** 收券的 CUSTOMER 用户。 */
    private Long targetUserId;
    /** 执行发券的后台用户。 */
    private Long operatorUserId;
    /** 调用方请求发放的数量。 */
    private Integer requestedQuantity;
    /** 实际成功数量；首版成功时等于 requestedQuantity。 */
    private Integer successQuantity;
    /** 后台填写的发放原因，便于审计。 */
    private String reason;
    /** 调用方生成的全局幂等键。 */
    private String idempotencyKey;
    /** 首版固定为 SUCCESS。 */
    private String status;
    /** 审计记录创建时间。 */
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
