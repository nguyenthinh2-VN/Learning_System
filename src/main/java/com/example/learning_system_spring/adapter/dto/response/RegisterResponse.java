package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.RegisterOutput;

import java.time.LocalDateTime;

public record RegisterResponse(
    Long id,
    String email,
    String name,
    String role,
    LocalDateTime createdAt
) {
    public static RegisterResponse from(RegisterOutput output) {
        return new RegisterResponse(
            output.id(), output.email(), output.name(), output.role(),
            output.createdAt()
        );
    }
}
