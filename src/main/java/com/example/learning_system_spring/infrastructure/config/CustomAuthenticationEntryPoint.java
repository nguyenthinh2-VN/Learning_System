package com.example.learning_system_spring.infrastructure.config;

import com.example.learning_system_spring.infrastructure.exception.ErrorCode;
import com.example.learning_system_spring.infrastructure.exception.ErrorResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String json = "{\n" +
                "  \"code\": \"INVALID_CREDENTIALS\",\n" +
                "  \"message\": \"Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.\",\n" +
                "  \"timestamp\": \"" + java.time.LocalDateTime.now().toString() + "\"\n" +
                "}";

        response.getWriter().write(json);
    }
}
