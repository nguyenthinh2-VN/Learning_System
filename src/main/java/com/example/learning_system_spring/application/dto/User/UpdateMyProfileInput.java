package com.example.learning_system_spring.application.dto.User;

public record UpdateMyProfileInput(
        Long userId,
        String name,
        String avatarUrl
) {}
