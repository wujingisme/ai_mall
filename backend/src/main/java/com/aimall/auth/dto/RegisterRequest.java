package com.aimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 消费者自助注册请求；Service 会强制把新用户角色设置为 CUSTOMER。 */
public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 64)
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "只能包含字母、数字和下划线") String username,
        // 当前版本只校验长度，不限制密码必须包含的字符类型。
        @NotBlank @Size(min = 6, max = 72) String password,
        @NotBlank @Size(max = 100) String displayName) {
}
