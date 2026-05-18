package com.example.learning_system_spring.application.dto.Course;

import com.example.learning_system_spring.domain.model.Role;

public record DeleteCourseInput(
                Long courseId,
                Long requesterId,
                Role requesterRole) {
}
