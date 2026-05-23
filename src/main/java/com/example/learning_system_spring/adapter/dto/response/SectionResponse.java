package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.Section.SectionOutput;

import java.util.List;
import java.util.stream.Collectors;

public record SectionResponse(
        Long id,
        String title,
        int orderIndex,
        List<LessonResponse> lessons) {

    public static SectionResponse from(SectionOutput output) {
        List<LessonResponse> lessonResponses = output.lessons().stream()
                .map(LessonResponse::from)
                .collect(Collectors.toList());

        return new SectionResponse(
                output.id(),
                output.title(),
                output.orderIndex(),
                lessonResponses);
    }
}
