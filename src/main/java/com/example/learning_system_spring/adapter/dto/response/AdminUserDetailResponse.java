package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.User.AdminUserDetailOutput;

import java.time.LocalDateTime;

public record AdminUserDetailResponse(
        Long id,
        String username,
        String email,
        String name,
        String role,
        boolean isInternal,
        boolean enabled,
        LocalDateTime createdAt
) {
    public static AdminUserDetailResponse from(AdminUserDetailOutput o) {
        return new AdminUserDetailResponse(
                o.id(), o.username(), o.email(), o.name(),
                o.role(), o.isInternal(), o.enabled(), o.createdAt());
    }
}
