package com.aimall.auth.exception;

/** 账号已被后台停用；统一映射为 HTTP 403。 */
public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException() { super("账号已禁用"); }
}
