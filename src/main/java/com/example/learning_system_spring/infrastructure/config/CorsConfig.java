package com.example.learning_system_spring.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Cho phép FE dev (IntelliJ built-in server, Vite, v.v.)
        config.setAllowedOrigins(List.of(
                "http://localhost:63342",   // IntelliJ built-in server
                "http://localhost:5173",    // Vite dev server
                "http://localhost:3000",    // CRA / Next.js
                "http://127.0.0.1:63342",
                "http://127.0.0.1:5173"
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);   // cần cho SockJS / cookie
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);  // áp dụng cho tất cả path kể cả /ws/**
        return source;
    }
}
