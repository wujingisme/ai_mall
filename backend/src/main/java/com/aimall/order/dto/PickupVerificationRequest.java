package com.aimall.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 后台取货核销请求。
 *
 * <p>取货码由用户首次创建订单时获得，店员只提交这一次输入；后端会先去除首尾空格、
 * 转成大写后计算 SHA-256，再与数据库摘要比较。数据库和响应都不会保存或返回明文取货码。</p>
 */
public record PickupVerificationRequest(
        @NotBlank(message = "取货码不能为空")
        @Size(min = 8, max = 8, message = "取货码必须是 8 位")
        @Pattern(
                regexp = "^[23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjklmnpqrstuvwxyz]+$",
                message = "取货码只能包含数字 2-9 和字母 A-Z（不区分大小写，排除 I/O）")
        String pickupCode) {
}
