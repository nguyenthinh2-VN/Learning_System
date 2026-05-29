package com.example.learning_system_spring.application.dto.User;

import com.example.learning_system_spring.domain.model.User;

import java.math.BigDecimal;

public record UserProfileOutput(
        Long id,
        String username,
        String email,
        String name,
        String role,
        boolean isInternal,
        BigDecimal balance,
        String avatarUrl
) {
    public static UserProfileOutput from(User user) {
        return new UserProfileOutput(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                user.getRole().getName(),
                user.isInternal(),
                user.getBalance(),
                user.getAvatarUrl()
        );
    }
}
