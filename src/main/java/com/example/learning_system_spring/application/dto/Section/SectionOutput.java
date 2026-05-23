package com.example.learning_system_spring.application.dto.Section;

import com.example.learning_system_spring.domain.model.CourseSection;

import java.util.List;
import java.util.stream.Collectors;

public record SectionOutput(
        Long id,
        String title,
        int orderIndex,
        List<LessonOutput> lessons) {

    public static SectionOutput from(CourseSection section) {
        List<LessonOutput> lessonOutputs = section.getLessons().stream()
                .map(LessonOutput::from)
                .collect(Collectors.toList());

        return new SectionOutput(
                section.getId(),
                section.getTitle(),
                section.getOrderIndex(),
                lessonOutputs);
    }
}
