package com.aimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(min = 3, max = 64) String username,
        // 与注册规则保持一致，允许使用任意字符组成的 6-72 位密码。
        @NotBlank @Size(min = 6, max = 72) String password) {
}
