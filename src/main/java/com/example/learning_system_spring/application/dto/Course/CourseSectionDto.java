package com.example.learning_system_spring.application.dto.Course;

import java.util.List;

public record CourseSectionDto(
        String title,
        int orderIndex,
        List<CourseLessonDto> lessons) {
}
