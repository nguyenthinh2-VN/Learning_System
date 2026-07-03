package com.example.learning_system_spring.application.dto.Course;

import com.example.learning_system_spring.domain.model.Role;

import java.util.List;
import java.math.BigDecimal;

public record CreateCourseInput(
                Long requesterId,
                Role requesterRole,
                String title,
                String description,
                int maxStudents,
                BigDecimal price,
                Long requestedInstructorId,
                String thumbnailUrl,
                boolean freeForInternal,
                boolean isMandatory,
                String assignedDepartment,
                java.time.LocalDateTime mandatoryDeadline,
                List<CourseSectionDto> sections) {
}
