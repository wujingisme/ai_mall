package com.aimall.config;

import com.aimall.common.error.ErrorResponse;
import com.aimall.auth.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
/**
 * Spring Security 安全边界。
 *
 * <p>这里定义哪些 URL 可以匿名访问、哪些需要登录、哪些需要角色；JWT 过滤器负责
 * 从请求头恢复身份。本地前端路由守卫只能改善体验，不能替代这里的服务端校验。</p>
 */
public class SecurityConfig {
    @Bean
    /** BCrypt 是单向密码摘要算法，注册和登录都通过同一个编码器处理密码。 */
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    /** 构建无状态的过滤链，并统一输出 JSON 格式的 401/403。 */
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper,
                                            JwtAuthenticationFilter jwtFilter) throws Exception {
        // Spring Security 默认偏向有 Session 的网站；商城 API 采用 Bearer Token，所以明确关闭状态会话。
        return http.csrf(csrf -> csrf.disable())
                // 让浏览器的 OPTIONS 预检使用 WebConfig 中允许的本地开发来源，避免在认证过滤链前被拦截。
                .cors(withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 注册、登录、刷新、退出和微信登录属于建立/结束会话的公开入口。
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/wechat/login",
                                "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                        .requestMatchers("/api/v1/coupon-shares/resolve").permitAll()
                        // 商品浏览和分享预览允许游客访问；领取和购物车仍必须登录。
                        .requestMatchers("/api/v1/shop/products/**").permitAll()
                        // 购物车按 JWT 中的用户 ID 隔离，游客不能读取或修改任何购物车数据。
                        .requestMatchers("/api/v1/cart/**").authenticated()
                        // 当前后台账号自助开通 CUSTOMER 只是给自己追加消费者能力，不会改变其他人的后台权限。
                        .requestMatchers("/api/v1/admin/accounts/me/customer-role")
                                .hasAnyRole("SUPER_ADMIN", "ADMIN", "OPERATOR")
                        // 创建后台账号属于高权限操作，只允许超级管理员调用。
                        .requestMatchers("/api/v1/admin/accounts/**").hasRole("SUPER_ADMIN")
                        // 优惠券模板属于运营配置，仅后台管理角色可查看和变更。
                        .requestMatchers("/api/v1/admin/coupon-templates/**")
                                .hasAnyRole("SUPER_ADMIN", "ADMIN", "OPERATOR")
                        .requestMatchers("/api/v1/admin/coupon-grants/**")
                                .hasAnyRole("SUPER_ADMIN", "ADMIN", "OPERATOR")
                        .requestMatchers("/api/v1/admin/customers/**")
                                .hasAnyRole("SUPER_ADMIN", "ADMIN", "OPERATOR")
                        // 订单列表和详情包含客户与订单经营数据，只允许后台管理角色访问。
                        .requestMatchers("/api/v1/admin/orders/**")
                                .hasAnyRole("SUPER_ADMIN", "ADMIN", "OPERATOR")
                        .requestMatchers("/api/v1/products/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "OPERATOR")
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of("UNAUTHORIZED", "未登录或访问令牌无效"));
                }).accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(403);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of("FORBIDDEN", "权限不足"));
                }))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
