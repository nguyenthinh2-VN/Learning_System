package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.LoginOutput;

import java.time.LocalDateTime;

public record LoginResponse(
    Long id,
    String email,
    String name,
    String role,
    String accessToken,
    LocalDateTime lastLogin
) {
    public static LoginResponse from(LoginOutput output) {
        return new LoginResponse(
            output.id(), output.email(), output.name(), output.role(),
            output.accessToken(), output.lastLogin()
        );
    }
}
