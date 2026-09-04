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
import com.aimall.coupon.exception.CouponShareException;
import com.aimall.order.exception.OrderNotFoundException;
import com.aimall.order.exception.OrderIdempotencyConflictException;
import com.aimall.order.exception.OrderRuleException;
import com.aimall.order.exception.OrderStockInsufficientException;
import com.aimall.common.error.ErrorResponse;
import com.aimall.common.error.FieldErrorDetail;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.*;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
/**
 * 全局异常到 HTTP 响应的统一转换器。
 *
 * <p>Service 只需要抛出有业务含义的异常；Spring 找到这里对应的处理方法后，
 * 会返回稳定的状态码和错误码。未知异常只记录服务端堆栈，客户端只看到通用 500，
 * 避免泄露数据库或内部实现细节。</p>
 */
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(WechatLoginException.class)
    /** 将微信适配器的失败分类映射为 401、503 等客户端可处理的响应。 */
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
    /** 无效刷新令牌统一返回 401，前端应清理会话并回到登录页。 */
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

    @ExceptionHandler(CouponShareException.class)
    ResponseEntity<ErrorResponse> handleCouponShare(CouponShareException e) {
        return ResponseEntity.status(e.isNotFound() ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT)
                .body(ErrorResponse.of(e.isNotFound() ? "COUPON_SHARE_NOT_FOUND" : "COUPON_SHARE_CONFLICT", e.getMessage()));
    }

    /** 订单 ID 不存在或不属于当前用户时统一返回 404，避免泄露订单归属。 */
    @ExceptionHandler(OrderNotFoundException.class)
    ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("ORDER_NOT_FOUND", e.getMessage()));
    }

    /** 订单预览参数或购物车归属不合法时返回 400。 */
    @ExceptionHandler(OrderRuleException.class)
    ResponseEntity<ErrorResponse> handleOrderRule(OrderRuleException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("ORDER_RULE_INVALID", e.getMessage()));
    }

    /** 预览阶段库存不足返回 409，提示前端刷新购物车后重试。 */
    @ExceptionHandler(OrderStockInsufficientException.class)
    ResponseEntity<ErrorResponse> handleOrderStock(OrderStockInsufficientException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("ORDER_STOCK_INSUFFICIENT", e.getMessage()));
    }

    /** 同一个幂等键提交了不同商品参数，返回 409 让前端生成新键后重新提交。 */
    @ExceptionHandler(OrderIdempotencyConflictException.class)
    ResponseEntity<ErrorResponse> handleOrderIdempotencyConflict(OrderIdempotencyConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("ORDER_IDEMPOTENCY_CONFLICT", e.getMessage()));
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    ResponseEntity<ErrorResponse> handleCustomerNotFound(CustomerNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of("USER_NOT_FOUND", e.getMessage()));
    }


    @ExceptionHandler({SkuConflictException.class, DuplicateKeyException.class})
    ResponseEntity<ErrorResponse> handleConflict(Exception e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of("SKU_CONFLICT", e.getMessage()));
    }

    /**
     * 后台修改商品总库存时，如果低于已被订单占用的数量，返回明确的库存冲突。
     *
     * <p>这不是“商品不存在”或“SKU 冲突”：管理员需要保留足够的总库存覆盖已有订单，
     * 否则后续取消/核销时会出现预留库存无法解释的状态。</p>
     */
    @ExceptionHandler(ProductStockConflictException.class)
    ResponseEntity<ErrorResponse> handleProductStockConflict(ProductStockConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("PRODUCT_STOCK_CONFLICT", e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    /** 汇总 DTO 字段校验错误，给前端表单逐字段展示提示。 */
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
    /** 最后的兜底处理：服务端记录完整堆栈，客户端不暴露内部异常消息。 */
    ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        Throwable rootCause = e;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        log.error("未处理请求异常: exceptionType={}, rootCauseType={}",
                e.getClass().getSimpleName(), rootCause.getClass().getSimpleName(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "服务器内部错误"));
    }
}
