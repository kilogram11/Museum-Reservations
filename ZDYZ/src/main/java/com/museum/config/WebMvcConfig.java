package com.museum.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//# Web MVC配置
//# - 跨域配置（CORS）
//# - 静态资源映射
//# - 拦截器注册（后面会用）
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射本地文件目录
        // file:files/ 表示项目根目录下的 files 文件夹
        String projectPath = System.getProperty("user.dir");
        String filesPath = projectPath + java.io.File.separator + "files" + java.io.File.separator;

        // 使用 toUri() 自动处理斜杠和特殊字符（如中文路径）
        String pathPattern = java.nio.file.Paths.get(filesPath).toUri().toString();

        registry.addResourceHandler("/files/**")
                .addResourceLocations(pathPattern);

        // 确保 static 目录下的静态资源（如 model_viewer.html, models/*.glb）能被访问
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
