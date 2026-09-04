package com.aimall.auth.service;

import com.aimall.auth.exception.WechatLoginException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.io.UncheckedIOException;

@Component
/**
 * 微信小程序 code2Session 的供应商适配器。
 *
 * <p>微信返回格式、错误码和响应头属于外部系统，不能让这些细节扩散到整个应用。
 * 本类负责配置检查、请求、限长读取、JSON 解析和错误分类，向 AuthService 只暴露
 * {@link WechatIdentity} 或统一的 {@link WechatLoginException}。</p>
 */
public class WechatMiniProgramClient {
    private static final Logger log = LoggerFactory.getLogger(WechatMiniProgramClient.class);
    private static final int MAX_RESPONSE_BYTES = 16 * 1024;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String appId;
    private final String appSecret;

    public WechatMiniProgramClient(RestClient.Builder builder, ObjectMapper objectMapper,
            @Value("${wechat.mini-program.app-id:}") String appId,
            @Value("${wechat.mini-program.app-secret:}") String appSecret) {
        this.restClient = builder.baseUrl("https://api.weixin.qq.com").build();
        this.objectMapper = objectMapper;
        this.appId = appId;
        this.appSecret = appSecret;
    }

    /**
     * 将小程序一次性 code 换成本地登录所需的 OpenID。
     *
     * <p>返回体限制为 16 KiB，既足够容纳微信正常响应，也避免异常上游返回超大内容。
     * 不记录 AppSecret、code、完整 URL 或外部 errmsg。</p>
     */
    public WechatIdentity exchangeCode(String code) {
        if (appId.isBlank() || appSecret.isBlank()) {
            throw new WechatLoginException(WechatLoginException.Failure.NOT_CONFIGURED,
                    "微信小程序登录尚未配置");
        }

        RawWechatResponse rawResponse = fetchRawResponse(code);
        if (rawResponse.statusCode() < 200 || rawResponse.statusCode() >= 300) {
            log.warn("微信 code2Session 返回异常 HTTP 状态: status={}", rawResponse.statusCode());
            throw serviceUnavailable();
        }
        if (rawResponse.body().length == 0 || rawResponse.body().length > MAX_RESPONSE_BYTES) {
            log.warn("微信 code2Session 返回异常响应长度: bodyLength={}", rawResponse.body().length);
            throw serviceUnavailable();
        }

        CodeSessionResponse response;
        try {
            // 微信及中间网关可能返回非标准 Content-Type；适配层读取限长原始字节后独立解析，
            // 避免依赖外部响应头，也不放宽应用全局的 HTTP 消息转换规则。
            response = objectMapper.readValue(rawResponse.body(), CodeSessionResponse.class);
        } catch (IOException e) {
            log.warn("微信 code2Session 返回无法解析的响应: exceptionType={}", e.getClass().getSimpleName());
            throw serviceUnavailable();
        }

        if (response.errorCode() != null && response.errorCode() != 0) {
            // 只记录微信错误码；errmsg 来自外部系统，不直接写日志或透传给客户端。
            log.warn("微信 code2Session 业务失败: errcode={}", response.errorCode());
            throw mapWechatError(response.errorCode());
        }
        if (response.openId() == null || response.openId().isBlank()) {
            log.warn("微信 code2Session 成功响应缺少 OpenID");
            throw serviceUnavailable();
        }
        return new WechatIdentity(response.openId(), response.unionId());
    }

    private RawWechatResponse fetchRawResponse(String code) {
        // code2Session 使用 GET 查询参数；这里是唯一接触 appSecret 的请求边界。
        try {
            return restClient.get().uri(uri -> uri.path("/sns/jscode2session")
                    .queryParam("appid", appId).queryParam("secret", appSecret)
                    .queryParam("js_code", code).queryParam("grant_type", "authorization_code")
                    .build()).exchange((request, response) -> readRawResponse(response));
        } catch (ResourceAccessException e) {
            // 请求 URL 带有 AppSecret 和一次性 code，禁止记录异常消息或完整 URL。
            log.warn("微信 code2Session 网络访问失败: exceptionType={}, rootCauseType={}",
                    e.getClass().getSimpleName(), rootCauseType(e));
            throw serviceUnavailable();
        } catch (Exception e) {
            log.warn("微信 code2Session 响应处理失败: exceptionType={}", e.getClass().getSimpleName());
            throw serviceUnavailable();
        }
    }

    private RawWechatResponse readRawResponse(ClientHttpResponse response) {
        // 多读取一个字节，用于识别“超过上限”的响应，而不是静默截断后误解析。
        try {
            return new RawWechatResponse(response.getStatusCode().value(),
                    response.getBody().readNBytes(MAX_RESPONSE_BYTES + 1));
        } catch (IOException e) {
            // exchange 回调不能传播读取状态或正文时的受检 IOException；保留原因为非受检异常，
            // 再由请求调用边界统一脱敏处理。
            throw new UncheckedIOException(e);
        }
    }

    private WechatLoginException mapWechatError(int errorCode) {
        // 将供应商错误码翻译成稳定的内部错误分类，Controller 不需要了解微信错误码。
        return switch (errorCode) {
            case 40029 -> new WechatLoginException(WechatLoginException.Failure.INVALID_CREDENTIAL,
                    "微信登录凭证无效或已过期");
            case 40013, 40125 -> new WechatLoginException(WechatLoginException.Failure.NOT_CONFIGURED,
                    "微信小程序登录配置无效");
            case -1, 45011 -> new WechatLoginException(WechatLoginException.Failure.SERVICE_UNAVAILABLE,
                    "微信登录服务繁忙，请稍后重试");
            default -> serviceUnavailable();
        };
    }

    private WechatLoginException serviceUnavailable() {
        return new WechatLoginException(WechatLoginException.Failure.SERVICE_UNAVAILABLE,
                "暂时无法连接微信登录服务");
    }

    private String rootCauseType(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        return root.getClass().getSimpleName();
    }

    /** 微信身份的最小内部表示；OpenID 只在服务端使用，不返回给前端。 */
    public record WechatIdentity(String openId, String unionId) {}
    private record RawWechatResponse(int statusCode, byte[] body) {}
    private record CodeSessionResponse(
            @JsonProperty("openid") String openId,
            @JsonProperty("unionid") String unionId,
            @JsonProperty("errcode") Integer errorCode,
            @JsonProperty("errmsg") String errorMessage) {}
}
