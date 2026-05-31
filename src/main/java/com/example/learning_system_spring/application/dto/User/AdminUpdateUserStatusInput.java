package com.example.learning_system_spring.application.dto.User;

/**
 * Input cho admin khóa/mở khóa tài khoản.
 * requesterId / requesterRole lấy từ JWT để áp dụng quy tắc bảo mật.
 */
public record AdminUpdateUserStatusInput(
        Long targetUserId,
        Long requesterId,
        String requesterRole,
        boolean enabled
) {}
