package com.example.learning_system_spring.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Phục vụ file tĩnh đã upload (avatar...) tại /uploads/** từ thư mục lưu trữ local.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final String baseDir;

    public WebMvcConfig(@Value("${storage.local.base-dir:./uploads}") String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(baseDir).toAbsolutePath().normalize();
        // Cần dấu "/" cuối để Spring hiểu đây là thư mục
        String location = uploadPath.toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
