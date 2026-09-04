package com.aimall.auth.exception;

/** 用户名违反唯一约束时的业务冲突异常，统一映射为 HTTP 409。 */
public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException() { super("用户名已存在"); }
}
