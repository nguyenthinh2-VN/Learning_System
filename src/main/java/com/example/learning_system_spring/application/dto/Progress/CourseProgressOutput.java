package com.example.learning_system_spring.application.dto.Progress;

import java.util.List;

public record CourseProgressOutput(
        Long courseId,
        int totalLessons,
        int completedLessons,
        int progressPercent,
        List<Long> completedLessonIds
) {}
