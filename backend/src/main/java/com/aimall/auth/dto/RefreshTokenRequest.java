package com.aimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 刷新/退出登录共用的请求体，令牌明文只在传输期间存在。 */
public record RefreshTokenRequest(@NotBlank @Size(min = 32) String refreshToken) {
}
