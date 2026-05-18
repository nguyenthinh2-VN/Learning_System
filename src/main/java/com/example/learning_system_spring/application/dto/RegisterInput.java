package com.example.learning_system_spring.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterInput(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6, max = 100) String password,
    @NotBlank @Size(min = 1, max = 200) String name
) {}
