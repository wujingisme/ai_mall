package com.aimall.auth.exception;

public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(Long id) { super("商城用户不存在：" + id); }
}
