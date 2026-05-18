package com.example.learning_system_spring.application.dto.Course;

import com.example.learning_system_spring.domain.model.Role;

import java.util.List;
import java.math.BigDecimal;

public record UpdateCourseInput(
                Long courseId,
                Long requesterId,
                Role requesterRole,
                String title,
                String description,
                int maxStudents,
                BigDecimal price,
                List<CourseSectionDto> sections) {
}
