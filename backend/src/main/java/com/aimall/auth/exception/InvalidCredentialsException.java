package com.aimall.auth.exception;

/** 用户名不存在或密码错误时的统一异常，防止账号枚举。 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() { super("用户名或密码错误"); }
}
