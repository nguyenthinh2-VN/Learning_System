package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.Lesson.GetLessonsOutput;
import java.util.List;

public record GetLessonsResponse(
    Long courseId,
    Long sectionId,
    List<LessonResponse> lessons
) {
    public static GetLessonsResponse from(GetLessonsOutput output) {
        List<LessonResponse> lessonResponses = output.lessons().stream()
                .map(LessonResponse::from)
                .toList();
        
        return new GetLessonsResponse(
            output.courseId(),
            output.sectionId(),
            lessonResponses
        );
    }
}