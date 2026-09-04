package com.aimall.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 正式创建订单请求。
 *
 * <p>clientRequestId 是前端为一次“提交订单”生成的幂等键；网络重试必须复用同一个值，
 * 后端才能把重复请求识别为同一笔订单，而不是再次锁库存。</p>
 */
public record OrderCreateRequest(
        @NotEmpty(message = "请至少选择一件商品")
        @Size(max = 50, message = "一次最多选择 50 种商品")
        List<@Valid OrderCreateItemRequest> items,
        @NotBlank(message = "幂等键不能为空")
        @Size(max = 64, message = "幂等键最多 64 个字符")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "幂等键只能包含字母、数字、下划线和短横线")
        String clientRequestId) {
}
