package com.aimall.auth.exception;

/** 微信登录适配器向应用层报告的可分类异常，不暴露供应商原始响应。 */
public class WechatLoginException extends RuntimeException {
    private final Failure failure;

    public WechatLoginException(Failure failure, String message) {
        super(message);
        this.failure = failure;
    }

    public Failure getFailure() { return failure; }

    public enum Failure {
        /** code 无效或已消费，客户端应重新调用 uni.login。 */
        INVALID_CREDENTIAL,
        /** 微信上游超时、异常或暂时不可用。 */
        SERVICE_UNAVAILABLE,
        /** 本地未配置或配置格式不合法。 */
        NOT_CONFIGURED
    }
}
