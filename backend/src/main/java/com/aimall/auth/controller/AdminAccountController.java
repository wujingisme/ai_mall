package com.aimall.auth.controller;

import com.aimall.auth.dto.AdminAccountCreateRequest;
import com.aimall.auth.service.AuthService;
import com.aimall.auth.vo.CurrentUserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/accounts")
/**
 * 后台账号相关入口。
 *
 * <p>创建其他后台账号仍然只允许超级管理员；“开通自己的消费者身份”不涉及给别人
 * 增权，因此允许已登录的后台角色调用，且 Controller 从 Authentication 读取自己的 ID。</p>
 */
public class AdminAccountController {
    private final AuthService authService;

    public AdminAccountController(AuthService authService) { this.authService = authService; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    /** 创建 ADMIN 或 OPERATOR 账号，不能通过此接口创建 SUPER_ADMIN。 */
    public CurrentUserResponse create(@Valid @RequestBody AdminAccountCreateRequest request) {
        // 路由权限由 SecurityConfig 强制校验为 SUPER_ADMIN，此处只处理账号创建。
        return authService.createAdminAccount(request);
    }

    /**
     * 为当前后台账号追加 CUSTOMER 角色。
     *
     * <p>路径不接收 userId，避免普通管理员借此修改其他账号；调用成功后，前台重新登录
     * 同一账号即可获得消费者端 JWT 角色。重复调用保持幂等，不会重复写入 CUSTOMER。</p>
     */
    @PostMapping("/me/customer-role")
    public CurrentUserResponse enableCustomerRole(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getPrincipal().toString());
        return authService.enableCustomerRole(userId);
    }
}
