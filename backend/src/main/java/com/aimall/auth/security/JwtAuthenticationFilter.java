package com.aimall.auth.security;

import com.aimall.auth.entity.MallUser;
import com.aimall.auth.mapper.MallUserMapper;
import com.aimall.auth.service.JwtService;
import com.aimall.common.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
/**
 * 每个 HTTP 请求只执行一次的 JWT 认证过滤器。
 *
 * <p>它位于 Controller 之前：读取 Authorization 请求头，验证令牌，
 * 再把用户 ID 和角色放入 Spring Security 的上下文。后续 Controller 通过
 * {@code Authentication} 读取这些信息，而不是相信请求体里的用户 ID。</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final MallUserMapper userMapper;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, MallUserMapper userMapper, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    /** 解析 Bearer 令牌；无效令牌不建立认证上下文，让受保护路由统一返回 401。 */
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        // 没有 Bearer 头时保持匿名，公开接口可以继续执行，受保护接口稍后由 SecurityConfig 拒绝。
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                // substring(7) 去掉固定前缀 "Bearer "，剩余内容才是 JWT 紧凑字符串。
                Claims claims = jwtService.parseAccessToken(authorization.substring(7));
                // 令牌签名有效不代表用户今天仍存在或启用，因此每次请求补一次数据库状态检查。
                MallUser user = userMapper.selectById(Long.valueOf(claims.getSubject()));
                if (user != null && !Boolean.TRUE.equals(user.getEnabled())) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of("ACCOUNT_DISABLED", "账号已禁用"));
                    return;
                }
                if (user == null) throw new IllegalArgumentException("用户不存在");
                List<?> roles = claims.get("roles", List.class);
                // Spring 的 hasRole("ADMIN") 会寻找 ROLE_ADMIN，所以这里补上 ROLE_ 前缀。
                var authorities = roles == null ? List.<SimpleGrantedAuthority>of() : roles.stream()
                        .map(String::valueOf).map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
                var authentication = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ignored) {
                // 无效令牌不写入认证上下文，受保护路由随后由 Spring Security 返回统一 401。
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
