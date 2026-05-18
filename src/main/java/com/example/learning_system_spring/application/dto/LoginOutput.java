package com.example.learning_system_spring.application.dto;

import com.example.learning_system_spring.domain.model.User;

import java.time.LocalDateTime;

public record LoginOutput(
    Long id,
    String email,
    String name,
    String role,
    String accessToken,
    LocalDateTime lastLogin
) {
    public static LoginOutput from(User user, String accessToken) {
        return new LoginOutput(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getRole().getName(),
            accessToken,
            LocalDateTime.now()
        );
    }
}
