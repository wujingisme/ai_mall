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
    private String roles;
    private Boolean enabled;
    private Integer failedLoginAttempts;
    private LocalDateTime lockedUntil;

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getRoles() { return roles; }
    public Boolean getEnabled() { return enabled; }
    public Integer getFailedLoginAttempts() { return failedLoginAttempts; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setFailedLoginAttempts(Integer value) { this.failedLoginAttempts = value; }
    public void setLockedUntil(LocalDateTime value) { this.lockedUntil = value; }
}
