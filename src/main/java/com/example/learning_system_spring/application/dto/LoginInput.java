package com.example.learning_system_spring.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginInput(
    @NotBlank @Email String email,
    @NotBlank String password
) {}
