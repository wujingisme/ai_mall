package com.aimall.auth.controller;

import com.aimall.auth.dto.LoginRequest;
import com.aimall.auth.service.AuthService;
import com.aimall.auth.vo.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
