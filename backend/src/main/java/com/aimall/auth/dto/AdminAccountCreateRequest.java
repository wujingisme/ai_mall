package com.aimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminAccountCreateRequest(
        @NotBlank @Size(min = 3, max = 64)
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "只能包含字母、数字和下划线") String username,
        @NotBlank @Size(min = 6, max = 72) String password,
        @NotBlank @Size(max = 100) String displayName,
        // 后台只允许创建日常管理角色，禁止通过该接口继续创建超级管理员。
        @NotBlank @Pattern(regexp = "ADMIN|OPERATOR", message = "角色只能是 ADMIN 或 OPERATOR") String role) {
}
