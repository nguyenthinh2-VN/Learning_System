package com.example.learning_system_spring.application.dto;

import com.example.learning_system_spring.domain.model.Course;

public record GetCourseDetailOutput(
        Long id,
        String title,
        String description,
        int maxStudents,
        int enrolledCount
) {
    public static GetCourseDetailOutput from(Course course) {
        return new GetCourseDetailOutput(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getMaxStudents(),
                course.getEnrolledCount()
        );
    }
}
