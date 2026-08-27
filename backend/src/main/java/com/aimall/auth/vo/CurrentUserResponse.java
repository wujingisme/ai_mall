package com.aimall.auth.vo;

import java.util.List;

public record CurrentUserResponse(String id, String username, String displayName,
                                  String avatarUrl, List<String> roles) {
}
