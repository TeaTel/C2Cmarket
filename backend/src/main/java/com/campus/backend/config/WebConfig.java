package com.campus.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 * 确保静态资源(前端Vue打包文件)可正常访问
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 静态资源映射 - 前端Vue.js打包后的文件
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}
