package com.aimall.auth.vo;

import java.util.List;

/** 返回给前端的当前用户公开资料，不包含密码、OpenID 等敏感字段。 */
public record CurrentUserResponse(String id, String username, String displayName,
                                  String avatarUrl, List<String> roles) {
}
