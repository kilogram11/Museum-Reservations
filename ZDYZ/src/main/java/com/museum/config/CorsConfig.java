package com.museum.config;
//# 跨域配置（如果单独拆出来）
//# - 允许前端跨域请求
//# - 配置允许的域名、方法、Header


import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置
 * 允许前端（Web管理端、小程序）跨域访问
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")                      // 所有接口都允许跨域
                .allowedOriginPatterns("*")             // 允许所有域名
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")                    // 允许所有请求头
                .allowCredentials(true)                 // 允许携带cookie
                .maxAge(3600);                          // 预检请求缓存时间
    }
}