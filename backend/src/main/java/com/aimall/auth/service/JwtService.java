package com.aimall.auth.service;

import com.aimall.auth.entity.MallUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

@Service
/**
 * JWT 访问令牌的唯一生成和解析位置。
 *
 * <p>JWT 可以理解为服务端签名的短期“通行证”。客户端可以保存并携带它，
 * 但不能修改其中的用户 ID 或角色后仍通过签名校验。</p>
 */
public class JwtService {
    private final SecretKey key;
    private final long expiresIn;

    public JwtService(@Value("${auth.jwt-secret}") String secret,
                      @Value("${auth.access-token-seconds}") long expiresIn) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiresIn = expiresIn;
    }

    /** 根据用户 ID、角色、签发时间和过期时间创建签名访问令牌。 */
    public String createAccessToken(MallUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("roles", Arrays.stream(user.getRoles().split(",")).map(String::trim).toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expiresIn)))
                .id(UUID.randomUUID().toString())
                .signWith(key)
                .compact();
    }

    /** 解析并验证签名和过期时间；任何失败都交给认证过滤器处理为未登录。 */
    public Claims parseAccessToken(String token) {
        // 解析过程同时校验签名、格式和过期时间，失败时由安全过滤器统一返回 401。
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
