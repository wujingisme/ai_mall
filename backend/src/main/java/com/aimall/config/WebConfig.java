package com.aimall.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
/** Web 层配置：本地开发跨域规则以及 Long ID 的字符串序列化策略。 */
public class WebConfig implements WebMvcConfigurer {
    @Override
    /** 只允许本地开发端口访问 /api，生产环境应由反向代理和正式域名控制来源。 */
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Bean
    /** 将 Long 序列化为字符串，避免 JavaScript Number 的 53 位精度限制。 */
    Jackson2ObjectMapperBuilderCustomizer longAsStringCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            builder.modulesToInstall(module);
        };
    }
}
