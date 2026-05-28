package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.User.UserListOutput;

import java.time.LocalDateTime;

public record UserListResponse(
        Long id,
        String username,
        String email,
        String name,
        String role,
        boolean isInternal,
        LocalDateTime createdAt
) {
    public static UserListResponse from(UserListOutput output) {
        return new UserListResponse(
                output.id(),
                output.username(),
                output.email(),
                output.name(),
                output.role(),
                output.isInternal(),
                output.createdAt()
        );
    }
}
