package com.aimall.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("mall_user")
public class MallUser {
    private Long id;
    private String username;
    private String passwordHash;
    private String displayName;
    private String avatarUrl;
    private String wechatOpenId;
    private String wechatUnionId;
    private String roles;
    private Boolean enabled;
    private Integer failedLoginAttempts;
    private LocalDateTime lockedUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getWechatOpenId() { return wechatOpenId; }
    public String getWechatUnionId() { return wechatUnionId; }
    public String getRoles() { return roles; }
    public Boolean getEnabled() { return enabled; }
    public Integer getFailedLoginAttempts() { return failedLoginAttempts; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setWechatOpenId(String value) { this.wechatOpenId = value; }
    public void setWechatUnionId(String value) { this.wechatUnionId = value; }
    public void setRoles(String roles) { this.roles = roles; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public void setFailedLoginAttempts(Integer value) { this.failedLoginAttempts = value; }
    public void setLockedUntil(LocalDateTime value) { this.lockedUntil = value; }
}
