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
public class JwtService {
    private final SecretKey key;
    private final long expiresIn;

    public JwtService(@Value("${auth.jwt-secret}") String secret,
                      @Value("${auth.access-token-seconds}") long expiresIn) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiresIn = expiresIn;
    }

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

    public Claims parseAccessToken(String token) {
        // 解析过程同时校验签名、格式和过期时间，失败时由安全过滤器统一返回 401。
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
