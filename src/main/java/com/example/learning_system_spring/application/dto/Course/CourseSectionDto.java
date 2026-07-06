package com.example.learning_system_spring.application.dto.Course;

import java.util.List;

public record CourseSectionDto(
        Long id,
        String title,
        int orderIndex,
        boolean hasTest,
        List<CourseLessonDto> lessons) {
}
