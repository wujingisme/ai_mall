package com.aimall.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("mall_user")
/** 商城用户实体；角色当前以逗号分隔字符串保存，密码字段保存 BCrypt 摘要。 */
public class MallUser {
    /** 数据库自增用户 ID，接口层通常序列化为字符串避免 JS 精度问题。 */
    private Long id;
    /** 密码登录用户名；数据库唯一。 */
    private String username;
    /** BCrypt 密码摘要，禁止返回给前端。 */
    private String passwordHash;
    /** 页面展示名称。 */
    private String displayName;
    /** 可选头像地址。 */
    private String avatarUrl;
    /** 微信小程序 OpenID；只在服务端识别微信用户。 */
    private String wechatOpenId;
    /** 微信 UnionID；可能为空，当前只做预留。 */
    private String wechatUnionId;
    /** 逗号分隔角色，例如 CUSTOMER 或 ADMIN。 */
    private String roles;
    /** 账号是否允许登录和调用受保护接口。 */
    private Boolean enabled;
    /** 连续密码失败次数。 */
    private Integer failedLoginAttempts;
    /** 临时锁定截止时间，空表示当前没有锁定。 */
    private LocalDateTime lockedUntil;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 最近修改时间。 */
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
