package com.aimall.auth.vo;

/** 登录/刷新成功响应：访问令牌用于请求，刷新令牌用于换取下一组令牌。 */
public record TokenResponse(String tokenType, String accessToken, long expiresIn,
                            String refreshToken, long refreshExpiresIn, CurrentUserResponse user) {
}
