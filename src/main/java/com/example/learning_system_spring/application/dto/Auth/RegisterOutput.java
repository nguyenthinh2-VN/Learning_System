package com.example.learning_system_spring.application.dto.Auth;

import com.example.learning_system_spring.domain.model.User;

import java.time.LocalDateTime;

public record RegisterOutput(
        Long id,
        String username,
        String email,
        String name,
        String role,
        boolean isInternal,
        LocalDateTime createdAt) {
    public static RegisterOutput from(User user) {
        return new RegisterOutput(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                user.getRole().getName(),
                user.isInternal(),
                user.getCreatedAt());
    }
}
