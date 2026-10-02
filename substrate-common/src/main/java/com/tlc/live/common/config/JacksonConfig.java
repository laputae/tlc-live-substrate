package com.tlc.live.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 显式提供 Jackson 2 的 ObjectMapper。
 * Spring Boot 4 默认自动装配 Jackson 3（tools.jackson），
 * 而 RocketMQ starter 与本工程的 JSON 序列化走 Jackson 2，需手动注册。
 * 所有微服务通过组件扫描（com.tlc.live）继承该配置。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}