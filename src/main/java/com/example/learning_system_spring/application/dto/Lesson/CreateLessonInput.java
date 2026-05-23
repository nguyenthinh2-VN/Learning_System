package com.example.learning_system_spring.application.dto.Lesson;

import com.example.learning_system_spring.domain.model.Role;

public record CreateLessonInput(
    Long courseId,
    Long sectionId,
    Long requesterId,
    Role requesterRole,
    String title,
    String contentUrl,
    int orderIndex
) {}