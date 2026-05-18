package com.example.learning_system_spring.application.usecase.strategy.course;

import com.example.learning_system_spring.domain.model.Role;
import org.springframework.stereotype.Component;

@Component
public class InstructorCourseStrategy implements CourseManagementStrategy {

    @Override
    public boolean supports(Role userRole) {
        return userRole.isInstructor();
    }

    @Override
    public Long resolveInstructorId(Long userId, Long requestedInstructorId) {
        // Giảng viên luôn tự tạo khóa học cho mình, bỏ qua requestedInstructorId
        return userId;
    }
}
