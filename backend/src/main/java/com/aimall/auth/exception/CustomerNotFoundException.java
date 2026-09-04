package com.aimall.auth.exception;

/** 目标不是 CUSTOMER 或不存在时使用的统一异常，避免泄露管理员账号。 */
public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(Long id) { super("商城用户不存在：" + id); }
}
