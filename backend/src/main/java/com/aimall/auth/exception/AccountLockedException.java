package com.aimall.auth.exception;

public class AccountLockedException extends RuntimeException {
    private final long retryAfter;
    public AccountLockedException(long retryAfter) {
        super("登录失败次数过多，账号暂时锁定");
        this.retryAfter = Math.max(1, retryAfter);
    }
    public long getRetryAfter() { return retryAfter; }
}
