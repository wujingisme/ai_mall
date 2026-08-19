package com.aimall.common.error;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(String code, String message, List<FieldErrorDetail> details, OffsetDateTime timestamp) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null, OffsetDateTime.now());
    }
}
