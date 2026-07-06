package com.example.learning_system_spring.application.dto.User;

import com.example.learning_system_spring.domain.model.User;

import java.math.BigDecimal;
import java.util.List;

public record UserProfileOutput(
        Long id,
        String username,
        String email,
        String name,
        String role,
        boolean isInternal,
        BigDecimal balance,
        String avatarUrl,
        Long departmentId,
        List<String> permissions) {
    public static UserProfileOutput from(User user) {
        return from(user, List.of());
    }

    public static UserProfileOutput from(User user, List<String> permissions) {
        return new UserProfileOutput(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                user.getRole().getName(),
                user.isInternal(),
                user.getBalance(),
                user.getAvatarUrl(),
                user.getDepartmentId(),
                permissions == null ? List.of() : permissions);
    }
}
