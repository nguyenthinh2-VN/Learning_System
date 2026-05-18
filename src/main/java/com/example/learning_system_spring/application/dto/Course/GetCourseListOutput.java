package com.example.learning_system_spring.application.dto.Course;

import com.example.learning_system_spring.domain.model.Course;
import java.math.BigDecimal;

public record GetCourseListOutput(
        Long id,
        String title,
        String description,
        int maxStudents,
        int enrolledCount,
        BigDecimal price,
        Long instructorId
) {
    public static GetCourseListOutput from(Course course) {
        return new GetCourseListOutput(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getMaxStudents(),
                course.getEnrolledCount(),
                course.getPrice(),
                course.getInstructorId()
        );
    }
}
