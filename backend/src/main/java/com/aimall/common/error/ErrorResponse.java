package com.aimall.common.error;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 所有业务错误返回的统一 JSON 结构。
 * code 供前端程序判断，message 供用户阅读，details 用于字段级校验错误。
 */
public record ErrorResponse(String code, String message, List<FieldErrorDetail> details, OffsetDateTime timestamp) {
    /** 创建没有字段明细的普通错误响应。 */
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null, OffsetDateTime.now());
    }
}
