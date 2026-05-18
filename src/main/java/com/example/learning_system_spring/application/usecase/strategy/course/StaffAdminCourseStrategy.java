package com.example.learning_system_spring.application.usecase.strategy.course;

import com.example.learning_system_spring.domain.model.Role;
import org.springframework.stereotype.Component;

@Component
public class StaffAdminCourseStrategy implements CourseManagementStrategy {

    @Override
    public boolean supports(Role userRole) {
        return userRole.isStaff() || userRole.isAdminUser() || userRole.isSuperAdmin();
    }

    @Override
    public Long resolveInstructorId(Long userId, Long requestedInstructorId) {
        if (requestedInstructorId == null) {
            throw new IllegalArgumentException("Vui lòng cung cấp instructorId khi Staff/Admin tạo khóa học.");
        }
        return requestedInstructorId;
    }
}
