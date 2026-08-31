package com.aimall.common.exception;

import com.aimall.auth.exception.AccountDisabledException;
import com.aimall.auth.exception.AccountLockedException;
import com.aimall.auth.exception.InvalidCredentialsException;
import com.aimall.auth.exception.UsernameAlreadyExistsException;
import com.aimall.auth.exception.RefreshTokenInvalidException;
import com.aimall.auth.exception.WechatLoginException;
import com.aimall.auth.exception.CustomerNotFoundException;
import com.aimall.cart.exception.CartItemNotFoundException;
import com.aimall.cart.exception.CartProductUnavailableException;
import com.aimall.coupon.exception.CouponTemplateNotFoundException;
import com.aimall.coupon.exception.CouponTemplateRuleException;
import com.aimall.coupon.exception.CouponTemplateStateConflictException;
import com.aimall.coupon.exception.CouponGrantConflictException;
import com.aimall.coupon.exception.CouponGrantRuleException;
import com.aimall.coupon.exception.UserCouponNotFoundException;
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
    @ExceptionHandler(WechatLoginException.class)
    ResponseEntity<ErrorResponse> handleWechatLogin(WechatLoginException e) {
        HttpStatus status;
        String code;
        switch (e.getFailure()) {
            case INVALID_CREDENTIAL -> {
                status = HttpStatus.UNAUTHORIZED;
                code = "WECHAT_LOGIN_FAILED";
            }
            case SERVICE_UNAVAILABLE -> {
                status = HttpStatus.SERVICE_UNAVAILABLE;
                code = "WECHAT_SERVICE_UNAVAILABLE";
            }
            case NOT_CONFIGURED -> {
                status = HttpStatus.SERVICE_UNAVAILABLE;
                code = "WECHAT_LOGIN_NOT_CONFIGURED";
            }
            default -> throw new IllegalStateException("未知微信登录失败类型");
        }
        return ResponseEntity.status(status).body(ErrorResponse.of(code, e.getMessage()));
    }
    @ExceptionHandler(RefreshTokenInvalidException.class)
    ResponseEntity<ErrorResponse> handleRefreshTokenInvalid(RefreshTokenInvalidException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("REFRESH_TOKEN_INVALID", e.getMessage()));
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    ResponseEntity<ErrorResponse> handleUsernameConflict(UsernameAlreadyExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("USERNAME_ALREADY_EXISTS", e.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("INVALID_CREDENTIALS", e.getMessage()));
    }

    @ExceptionHandler(AccountDisabledException.class)
    ResponseEntity<ErrorResponse> handleAccountDisabled(AccountDisabledException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("ACCOUNT_DISABLED", e.getMessage()));
    }

    @ExceptionHandler(AccountLockedException.class)
    ResponseEntity<ErrorResponse> handleAccountLocked(AccountLockedException e) {
        return ResponseEntity.status(HttpStatus.LOCKED)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(e.getRetryAfter()))
                .body(ErrorResponse.of("ACCOUNT_LOCKED", e.getMessage()));
    }

    @ExceptionHandler(ProductNotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(ProductNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of("PRODUCT_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(CartItemNotFoundException.class)
    ResponseEntity<ErrorResponse> handleCartItemNotFound(CartItemNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of("CART_ITEM_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(CartProductUnavailableException.class)
    ResponseEntity<ErrorResponse> handleCartProductUnavailable(CartProductUnavailableException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of("CART_PRODUCT_UNAVAILABLE", e.getMessage()));
    }

    @ExceptionHandler(CouponTemplateNotFoundException.class)
    ResponseEntity<ErrorResponse> handleCouponTemplateNotFound(CouponTemplateNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("COUPON_TEMPLATE_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(CouponTemplateRuleException.class)
    ResponseEntity<ErrorResponse> handleCouponTemplateRule(CouponTemplateRuleException e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("COUPON_TEMPLATE_RULE_INVALID", e.getMessage()));
    }

    @ExceptionHandler(CouponTemplateStateConflictException.class)
    ResponseEntity<ErrorResponse> handleCouponTemplateStateConflict(CouponTemplateStateConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("COUPON_TEMPLATE_STATE_CONFLICT", e.getMessage()));
    }

    @ExceptionHandler(CouponGrantRuleException.class)
    ResponseEntity<ErrorResponse> handleCouponGrantRule(CouponGrantRuleException e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("COUPON_GRANT_RULE_INVALID", e.getMessage()));
    }

    @ExceptionHandler(CouponGrantConflictException.class)
    ResponseEntity<ErrorResponse> handleCouponGrantConflict(CouponGrantConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("COUPON_GRANT_CONFLICT", e.getMessage()));
    }

    @ExceptionHandler(UserCouponNotFoundException.class)
    ResponseEntity<ErrorResponse> handleUserCouponNotFound(UserCouponNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("USER_COUPON_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    ResponseEntity<ErrorResponse> handleCustomerNotFound(CustomerNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of("USER_NOT_FOUND", e.getMessage()));
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
