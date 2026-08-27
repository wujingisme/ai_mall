package com.aimall.auth.exception;

public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException() { super("账号已禁用"); }
}
