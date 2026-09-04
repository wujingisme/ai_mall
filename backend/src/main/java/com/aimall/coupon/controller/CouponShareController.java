package com.aimall.coupon.controller;

import com.aimall.coupon.dto.CouponShareCreateRequest;
import com.aimall.coupon.dto.CouponTokenRequest;
import com.aimall.coupon.service.CouponShareService;
import com.aimall.coupon.vo.CouponShareCreateResponse;
import com.aimall.coupon.vo.CouponShareResolveResponse;
import com.aimall.coupon.vo.UserCouponResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 分享创建、公开解析和登录后领取三个接口的 HTTP 入口。 */
@RestController
public class CouponShareController {
    private final CouponShareService service;

    /** Spring 通过构造函数注入分享业务服务，便于替换实现和单元测试。 */
    public CouponShareController(CouponShareService service) {
        this.service = service;
    }

    /** 当前用户把自己的一张可用券生成一次性分享凭证。 */
    @PostMapping("/api/v1/me/coupon-shares")
    ResponseEntity<CouponShareCreateResponse> create(
            Authentication authentication,
            @Valid @RequestBody CouponShareCreateRequest request) {
        // principal 是 JWT 过滤器放入的用户 ID，不能从请求体接收 creatorUserId。
        Long userId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(userId, request));
    }

    /** 无需登录即可解析分享预览，但不直接发券。 */
    @PostMapping("/api/v1/coupon-shares/resolve")
    CouponShareResolveResponse resolve(@Valid @RequestBody CouponTokenRequest request) {
        return service.resolve(request);
    }

    /** 登录用户领取分享券；用户身份从 JWT 获取。 */
    @PostMapping("/api/v1/coupon-claims")
    UserCouponResponse claim(
            Authentication authentication,
            @Valid @RequestBody CouponTokenRequest request) {
        Long userId = Long.valueOf(authentication.getPrincipal().toString());
        return service.claim(userId, request);
    }
}
