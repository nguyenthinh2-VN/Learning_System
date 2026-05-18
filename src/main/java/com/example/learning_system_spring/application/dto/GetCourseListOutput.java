package com.example.learning_system_spring.application.dto;

import com.example.learning_system_spring.domain.model.Course;

public record GetCourseListOutput(
        Long id,
        String title,
        String description,
        int maxStudents,
        int enrolledCount
) {
    public static GetCourseListOutput from(Course course) {
        return new GetCourseListOutput(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getMaxStudents(),
                course.getEnrolledCount()
        );
    }
}
