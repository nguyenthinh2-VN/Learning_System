package com.example.learning_system_spring.application.dto.Auth;

import jakarta.validation.constraints.NotBlank;

public record LoginInput(
    @NotBlank String identifier,
    @NotBlank String password
) {}
