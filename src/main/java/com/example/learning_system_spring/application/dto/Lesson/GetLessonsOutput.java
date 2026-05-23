package com.example.learning_system_spring.application.dto.Lesson;

import com.example.learning_system_spring.application.dto.Section.LessonOutput;
import java.util.List;

public record GetLessonsOutput(
    Long courseId,
    Long sectionId,
    List<LessonOutput> lessons
) {
    public static GetLessonsOutput of(Long courseId, Long sectionId, List<LessonOutput> lessons) {
        return new GetLessonsOutput(courseId, sectionId, lessons);
    }
}