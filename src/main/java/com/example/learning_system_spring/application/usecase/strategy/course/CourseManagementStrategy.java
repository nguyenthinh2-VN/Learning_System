package com.example.learning_system_spring.application.usecase.strategy.course;

import com.example.learning_system_spring.domain.model.Role;

public interface CourseManagementStrategy {
    boolean supports(Role userRole);
    Long resolveInstructorId(Long userId, Long requestedInstructorId);
}
