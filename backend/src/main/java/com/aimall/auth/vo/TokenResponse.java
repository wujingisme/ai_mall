package com.aimall.auth.vo;

public record TokenResponse(String tokenType, String accessToken, long expiresIn,
                            String refreshToken, long refreshExpiresIn, CurrentUserResponse user) {
}
