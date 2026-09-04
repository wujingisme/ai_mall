package com.aimall.auth.exception;

/** 双角色账号仍承担后台职责时，禁止把客户启停操作当成后台账号管理操作。 */
public class CustomerManagementConflictException extends RuntimeException {
    public CustomerManagementConflictException() {
        super("后台账号不能执行客户启用或停用操作");
    }
}
