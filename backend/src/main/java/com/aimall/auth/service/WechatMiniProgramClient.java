package com.aimall.auth.service;

import com.aimall.auth.exception.WechatLoginException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class WechatMiniProgramClient {
    private final RestClient restClient;
    private final String appId;
    private final String appSecret;

    public WechatMiniProgramClient(RestClient.Builder builder,
            @Value("${wechat.mini-program.app-id:}") String appId,
            @Value("${wechat.mini-program.app-secret:}") String appSecret) {
        this.restClient = builder.baseUrl("https://api.weixin.qq.com").build();
        this.appId = appId;
        this.appSecret = appSecret;
    }

    public WechatIdentity exchangeCode(String code) {
        if (appId.isBlank() || appSecret.isBlank()) {
            throw new WechatLoginException("微信小程序登录尚未配置");
        }
        CodeSessionResponse response;
        try {
            response = restClient.get().uri(uri -> uri.path("/sns/jscode2session")
                    .queryParam("appid", appId).queryParam("secret", appSecret)
                    .queryParam("js_code", code).queryParam("grant_type", "authorization_code")
                    .build()).retrieve().body(CodeSessionResponse.class);
        } catch (Exception e) {
            throw new WechatLoginException("暂时无法连接微信登录服务");
        }
        if (response == null || response.openId() == null || response.openId().isBlank()) {
            throw new WechatLoginException(response != null && response.errorMessage() != null
                    ? "微信登录失败：" + response.errorMessage() : "微信登录凭证无效或已过期");
        }
        return new WechatIdentity(response.openId(), response.unionId());
    }

    public record WechatIdentity(String openId, String unionId) {}
    private record CodeSessionResponse(
            @JsonProperty("openid") String openId,
            @JsonProperty("unionid") String unionId,
            @JsonProperty("errcode") Integer errorCode,
            @JsonProperty("errmsg") String errorMessage) {}
}
