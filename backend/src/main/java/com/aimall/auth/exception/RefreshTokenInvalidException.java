package com.aimall.auth.exception;

public class RefreshTokenInvalidException extends RuntimeException {
    public RefreshTokenInvalidException() { super("刷新令牌无效或已过期"); }
}
