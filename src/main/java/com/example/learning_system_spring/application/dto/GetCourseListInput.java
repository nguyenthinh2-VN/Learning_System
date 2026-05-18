package com.example.learning_system_spring.application.dto;

public record GetCourseListInput(
        String keyword,
        int page,
        int size
) {
}
