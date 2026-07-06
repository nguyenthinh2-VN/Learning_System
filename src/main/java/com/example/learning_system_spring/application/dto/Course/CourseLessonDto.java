package com.example.learning_system_spring.application.dto.Course;

public record CourseLessonDto(
                Long id,
                String title,
                String contentUrl,
                int orderIndex) {
}
