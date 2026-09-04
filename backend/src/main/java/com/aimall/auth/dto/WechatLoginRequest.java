package com.aimall.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** 微信小程序登录请求；code 是 uni.login 返回的一次性临时凭证。 */
public record WechatLoginRequest(@NotBlank(message = "微信登录 code 不能为空") String code) {}
