package com.example.learning_system_spring.application.dto.User;

import java.time.LocalDateTime;

public record UserListOutput(
        Long id,
        String username,
        String email,
        String name,
        String role,
        boolean isInternal,
        LocalDateTime createdAt
) {}
