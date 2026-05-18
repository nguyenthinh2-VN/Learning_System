package com.example.learning_system_spring.application.dto.Auth;

import com.example.learning_system_spring.domain.model.User;

import java.time.LocalDateTime;

public record LoginOutput(
    Long id,
    String username,
    String email,
    String name,
    String role,
    boolean isInternal,
    String accessToken,
    LocalDateTime lastLogin
) {
    public static LoginOutput from(User user, String accessToken) {
        return new LoginOutput(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getName(),
            user.getRole().getName(),
            user.isInternal(),
            accessToken,
            LocalDateTime.now()
        );
    }
}
