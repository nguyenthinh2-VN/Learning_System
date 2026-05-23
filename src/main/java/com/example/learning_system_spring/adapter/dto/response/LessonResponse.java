package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.Section.LessonOutput;

public record LessonResponse(
        Long id,
        String title,
        String contentUrl,
        int orderIndex) {

    public static LessonResponse from(LessonOutput output) {
        return new LessonResponse(
                output.id(),
                output.title(),
                output.contentUrl(),
                output.orderIndex());
    }
}
