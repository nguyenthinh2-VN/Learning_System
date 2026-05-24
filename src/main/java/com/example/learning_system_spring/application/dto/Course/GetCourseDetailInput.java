package com.example.learning_system_spring.application.dto.Course;

import com.example.learning_system_spring.domain.model.Role;

/**
 * Input cho GetCourseDetailUseCase. requesterId / requesterRole có thể null nếu là
 * public request (chưa đăng nhập). Nếu null → chỉ cho phép xem course đã publish.
 */
public record GetCourseDetailInput(
        Long id,
        Long requesterId,
        Role requesterRole
) {
    public static GetCourseDetailInput anonymous(Long id) {
        return new GetCourseDetailInput(id, null, null);
    }
}
