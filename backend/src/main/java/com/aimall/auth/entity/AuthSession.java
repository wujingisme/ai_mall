package com.aimall.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("auth_session")
public class AuthSession {
    private String id;
    private Long userId;
    private String refreshTokenHash;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public Long getUserId() { return userId; }
    public String getRefreshTokenHash() { return refreshTokenHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setId(String id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setRefreshTokenHash(String hash) { this.refreshTokenHash = hash; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
