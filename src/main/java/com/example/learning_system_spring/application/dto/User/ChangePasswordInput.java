package com.example.learning_system_spring.application.dto.User;

public record ChangePasswordInput(
        Long userId,
        String currentPassword,
        String newPassword
) {}
