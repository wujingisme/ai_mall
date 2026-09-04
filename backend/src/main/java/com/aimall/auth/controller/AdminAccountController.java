package com.aimall.auth.controller;

import com.aimall.auth.dto.AdminAccountCreateRequest;
import com.aimall.auth.service.AuthService;
import com.aimall.auth.vo.CurrentUserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/accounts")
/** 仅负责接收超级管理员创建后台账号的请求；角色权限由 SecurityConfig 先行拦截。 */
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
}
