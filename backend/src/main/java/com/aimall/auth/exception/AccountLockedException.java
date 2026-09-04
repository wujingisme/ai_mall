package com.aimall.auth.exception;

/** 登录失败次数达到阈值后的临时锁定异常，携带 Retry-After 秒数。 */
public class AccountLockedException extends RuntimeException {
    private final long retryAfter;
    public AccountLockedException(long retryAfter) {
        super("登录失败次数过多，账号暂时锁定");
        this.retryAfter = Math.max(1, retryAfter);
    }
    public long getRetryAfter() { return retryAfter; }
}
