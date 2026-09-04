package com.aimall.coupon.dto;

import jakarta.validation.constraints.*;

/** 人工发券请求；幂等键由调用方生成并在数据库中全局唯一。 */
public record CouponGrantRequest(
        @NotNull(message = "优惠券模板不能为空") @Positive(message = "优惠券模板不合法") Long templateId,
        @NotNull(message = "目标用户不能为空") @Positive(message = "目标用户不合法") Long targetUserId,
        @NotNull(message = "发放数量不能为空") @Min(value = 1, message = "发放数量至少为 1") @Max(value = 100, message = "单次最多发放 100 张") Integer quantity,
        @NotBlank(message = "发放原因不能为空") @Size(max = 200, message = "发放原因最多 200 个字符") String reason,
        @NotBlank(message = "幂等键不能为空") @Size(max = 64, message = "幂等键最多 64 个字符")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "幂等键只能包含字母、数字、下划线和短横线") String idempotencyKey
) {}
