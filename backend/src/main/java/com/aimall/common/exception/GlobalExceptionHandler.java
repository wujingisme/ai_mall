package com.aimall.common.exception;

import com.aimall.common.error.ErrorResponse;
import com.aimall.common.error.FieldErrorDetail;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.*;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ProductNotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(ProductNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of("PRODUCT_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler({SkuConflictException.class, DuplicateKeyException.class})
    ResponseEntity<ErrorResponse> handleConflict(Exception e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of("SKU_CONFLICT", e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    ResponseEntity<ErrorResponse> handleInvalidBody(BindException e) {
        List<FieldErrorDetail> details = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage())).toList();
        return ResponseEntity.badRequest().body(
                new ErrorResponse("VALIDATION_ERROR", "请求参数校验失败", details, OffsetDateTime.now()));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErrorResponse> handleInvalidParameter(HandlerMethodValidationException e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("VALIDATION_ERROR", "请求参数校验失败"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "服务器内部错误"));
    }
}
