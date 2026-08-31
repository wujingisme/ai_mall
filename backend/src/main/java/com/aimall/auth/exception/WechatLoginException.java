package com.aimall.auth.exception;

public class WechatLoginException extends RuntimeException {
    private final Failure failure;

    public WechatLoginException(Failure failure, String message) {
        super(message);
        this.failure = failure;
    }

    public Failure getFailure() { return failure; }

    public enum Failure {
        INVALID_CREDENTIAL,
        SERVICE_UNAVAILABLE,
        NOT_CONFIGURED
    }
}
