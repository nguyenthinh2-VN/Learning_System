package com.example.learning_system_spring.application.dto.User;

import com.example.learning_system_spring.domain.model.User;

import java.time.LocalDateTime;

public record AdminUserDetailOutput(
        Long id,
        String username,
        String email,
        String name,
        String role,
        boolean isInternal,
        boolean enabled,
        Long departmentId,
        LocalDateTime createdAt
) {
    public static AdminUserDetailOutput from(User user) {
        return new AdminUserDetailOutput(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                user.getRole().getName(),
                user.isInternal(),
                user.isEnabled(),
                user.getDepartmentId(),
                user.getCreatedAt());
    }
}
