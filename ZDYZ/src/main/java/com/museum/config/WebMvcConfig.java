package com.museum.config;

import com.museum.security.AdminAuthInterceptor;
import com.museum.security.AppAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：静态资源、CORS、JWT 拦截器注册。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AppAuthInterceptor appAuthInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;

    public WebMvcConfig(AppAuthInterceptor appAuthInterceptor, AdminAuthInterceptor adminAuthInterceptor) {
        this.appAuthInterceptor = appAuthInterceptor;
        this.adminAuthInterceptor = adminAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(appAuthInterceptor)
                .addPathPatterns("/app/**", "/ai/**");

        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin/**", "/stats/**", "/message/**")
                .excludePathPatterns("/admin/auth/login", "/admin/auth/register");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String projectPath = System.getProperty("user.dir");
        String filesPath = projectPath + java.io.File.separator + "files" + java.io.File.separator;

        String pathPattern = java.nio.file.Paths.get(filesPath).toUri().toString();

        registry.addResourceHandler("/files/**")
                .addResourceLocations(pathPattern);

        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }

    @Override
    public void addCorsMappings(org.springframework.web.servlet.config.annotation.CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
