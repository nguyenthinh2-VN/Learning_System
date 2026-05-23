package com.example.learning_system_spring.application.dto.Lesson;

public record GetLessonsInput(
    Long courseId,
    Long sectionId
) {}