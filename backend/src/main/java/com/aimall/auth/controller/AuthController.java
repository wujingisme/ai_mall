package com.aimall.auth.controller;

import com.aimall.auth.dto.LoginRequest;
import com.aimall.auth.dto.RegisterRequest;
import com.aimall.auth.dto.RefreshTokenRequest;
import com.aimall.auth.dto.WechatLoginRequest;
import com.aimall.auth.service.AuthService;
import com.aimall.auth.vo.CurrentUserResponse;
import com.aimall.auth.vo.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
/**
 * 认证相关的 HTTP 入口。
 *
 * <p>Controller 类似前端的 API client 路由层：负责把 HTTP 请求参数接到 Java 对象，
 * 然后交给 {@link AuthService}。真正的密码校验、令牌生成和数据库写入都在 Service 中，
 * 这样接口层不会堆积业务规则。</p>
 */
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    /** 接收消费者注册请求；{@code @Valid} 会先执行 DTO 上的基础字段校验。 */
    public CurrentUserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    /** 使用用户名和密码登录，成功后返回短期访问令牌和长期刷新令牌。 */
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/wechat/login")
    /** 接收小程序的一次性 code；OpenID 交换和用户复用由 Service/微信适配器完成。 */
    public TokenResponse wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        return authService.loginWithWechat(request.code());
    }

    @PostMapping("/refresh")
    /** 使用刷新令牌轮换一组新令牌，旧刷新令牌会被撤销，防止重复使用。 */
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    /** 撤销刷新令牌；接口设计为幂等，重复退出仍然返回成功。 */
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
    }

    @GetMapping("/me")
    /** 返回当前 Bearer 访问令牌对应的用户公开信息。 */
    public CurrentUserResponse me(Authentication authentication) {
        return authService.currentUser(authentication.getPrincipal().toString());
    }
}
