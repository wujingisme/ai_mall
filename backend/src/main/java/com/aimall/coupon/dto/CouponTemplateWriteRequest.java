package com.aimall.coupon.dto;

import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;

/** 优惠券模板的写入请求；金额用字符串传输，避免前端浮点数精度问题。 */
public record CouponTemplateWriteRequest(
        @NotBlank(message = "优惠券名称不能为空") @Size(max = 100, message = "优惠券名称最多 100 个字符") String name,
        @NotBlank(message = "优惠券类型不能为空") String couponType,
        @NotBlank(message = "使用门槛不能为空")
        @Pattern(regexp = "^(0|[1-9]\\d{0,7})(\\.\\d{1,2})?$", message = "使用门槛必须是最多 8 位整数和 2 位小数的非负金额") String minimumSpend,
        @NotBlank(message = "优惠金额不能为空")
        @Pattern(regexp = "^(0|[1-9]\\d{0,7})(\\.\\d{1,2})?$", message = "优惠金额必须是最多 8 位整数和 2 位小数的非负金额") String discountAmount,
        @NotNull(message = "发行总量不能为空") @Min(value = 1, message = "发行总量至少为 1") Integer totalQuantity,
        @NotNull(message = "每人限领数量不能为空") @Min(value = 1, message = "每人限领数量至少为 1") Integer perUserLimit,
        @NotBlank(message = "有效期类型不能为空") String validityType,
        OffsetDateTime validFrom,
        OffsetDateTime validUntil,
        @Min(value = 1, message = "领取后有效天数至少为 1") @Max(value = 3650, message = "领取后有效天数不能超过 3650") Integer validDays,
        @NotNull(message = "是否允许分享不能为空") Boolean shareEnabled
) {}
