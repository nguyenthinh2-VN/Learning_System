package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.Auth.RegisterOutput;

import java.time.LocalDateTime;

public record RegisterResponse(
    Long id,
    String username,
    String email,
    String name,
    String role,
    boolean isInternal,
    LocalDateTime createdAt
) {
    public static RegisterResponse from(RegisterOutput output) {
        return new RegisterResponse(
            output.id(), output.username(), output.email(), output.name(), output.role(), output.isInternal(),
            output.createdAt()
        );
    }
}
