package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.Auth.LoginOutput;

import java.time.LocalDateTime;

public record LoginResponse(
    Long id,
    String username,
    String email,
    String name,
    String role,
    boolean isInternal,
    String accessToken,
    LocalDateTime lastLogin
) {
    public static LoginResponse from(LoginOutput output) {
        return new LoginResponse(
            output.id(), output.username(), output.email(), output.name(), output.role(), output.isInternal(),
            output.accessToken(), output.lastLogin()
        );
    }
}
