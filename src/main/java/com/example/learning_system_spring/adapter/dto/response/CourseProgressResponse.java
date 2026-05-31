package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.Progress.CourseProgressOutput;

import java.util.List;

public record CourseProgressResponse(
        Long courseId,
        int totalLessons,
        int completedLessons,
        int progressPercent,
        List<Long> completedLessonIds
) {
    public static CourseProgressResponse from(CourseProgressOutput o) {
        return new CourseProgressResponse(
                o.courseId(), o.totalLessons(), o.completedLessons(),
                o.progressPercent(), o.completedLessonIds());
    }
}
