package com.aimall.coupon.exception;
/** 分享不存在/过期或领取条件冲突；notFound 决定统一处理器返回 404 还是 409。 */
public class CouponShareException extends RuntimeException {
    private final boolean notFound;

    /** @param message 给前端展示的稳定业务消息 @param notFound 是否应映射为 404 */
    public CouponShareException(String message, boolean notFound) {
        super(message);
        this.notFound = notFound;
    }

    /** 供 GlobalExceptionHandler 判断返回 404 还是 409。 */
    public boolean isNotFound() {
        return notFound;
    }
}
