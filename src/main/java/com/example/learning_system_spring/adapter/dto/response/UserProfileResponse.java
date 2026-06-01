package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.User.UserProfileOutput;

import java.math.BigDecimal;
import java.util.List;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String name,
        String role,
        boolean isInternal,
        BigDecimal balance,
        String avatarUrl,
        List<String> permissions
) {
    public static UserProfileResponse from(UserProfileOutput output) {
        return new UserProfileResponse(
                output.id(),
                output.username(),
                output.email(),
                output.name(),
                output.role(),
                output.isInternal(),
                output.balance(),
                output.avatarUrl(),
                output.permissions()
        );
    }
}
