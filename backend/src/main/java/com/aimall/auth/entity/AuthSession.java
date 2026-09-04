package com.aimall.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("auth_session")
/** 刷新令牌会话实体；数据库只保存 refresh token 的摘要，revokedAt 不为空表示已撤销。 */
public class AuthSession {
    /** 会话 UUID；用于日志和条件撤销，不是客户端携带的刷新令牌。 */
    private String id;
    /** 会话所属用户的主键。 */
    private Long userId;
    /** 刷新令牌的 SHA-256 十六进制摘要。 */
    private String refreshTokenHash;
    /** 刷新令牌失效时间。 */
    private LocalDateTime expiresAt;
    /** 撤销时间；非空表示已经退出或完成轮换。 */
    private LocalDateTime revokedAt;
    /** 会话创建时间。 */
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
