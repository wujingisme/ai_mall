package com.aimall.auth.exception;

/** 刷新令牌不存在、已撤销或已过期时的认证异常。 */
public class RefreshTokenInvalidException extends RuntimeException {
    public RefreshTokenInvalidException() { super("刷新令牌无效或已过期"); }
}
