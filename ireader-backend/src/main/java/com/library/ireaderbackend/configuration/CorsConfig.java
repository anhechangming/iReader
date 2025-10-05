package com.library.ireaderbackend.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // 对所有接口生效
                        .allowedOrigins("http://localhost:5173") // 允许的前端源（生产环境建议明确指定，不要用*）
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 明确允许的方法（包含OPTIONS预检请求）
                        .allowedHeaders("*") // 允许所有请求头（如果有自定义头，也可以明确指定，如"Content-Type, Token"）
                        .allowCredentials(true) // 允许携带凭证（如Cookie，前端请求也需要配置withCredentials: true）
                        .maxAge(3600); // 预检请求的有效期（秒），避免频繁预检
            }
        };
    }
}