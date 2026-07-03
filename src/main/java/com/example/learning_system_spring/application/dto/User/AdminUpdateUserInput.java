package com.example.learning_system_spring.application.dto.User;

/**
 * Input cho admin cập nhật user. Các field nullable = giữ nguyên giá trị cũ.
 * requesterId / requesterRole lấy từ JWT để áp dụng quy tắc bảo mật.
 */
public record AdminUpdateUserInput(
        Long targetUserId,
        Long requesterId,
        String requesterRole,
        String name,
        String roleName,
        Boolean isInternal,
        String department
) {}
