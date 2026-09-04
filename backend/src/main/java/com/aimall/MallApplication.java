package com.aimall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
/**
 * AI Mall 后端的启动入口。
 *
 * <p>Spring Boot 会从这个类所在的 {@code com.aimall} 包开始扫描组件，
 * 自动发现 Controller、Service、Mapper 和配置类。前端不需要直接调用这个类；
 * 执行 {@code mvn spring-boot:run} 后，Spring 才会根据这些类创建 HTTP 服务。</p>
 */
public class MallApplication {
    /** 启动 Spring 容器，并开始监听 application.yml 配置的端口。 */
    public static void main(String[] args) {
        SpringApplication.run(MallApplication.class, args);
    }
}
