package com.example.learning_system_spring.application.dto.Section;

import com.example.learning_system_spring.domain.model.CourseLesson;

public record LessonOutput(
        Long id,
        String title,
        String contentUrl,
        int orderIndex) {

    public static LessonOutput from(CourseLesson lesson) {
        return new LessonOutput(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getContentUrl(),
                lesson.getOrderIndex());
    }
}
