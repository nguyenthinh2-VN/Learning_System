package com.example.learning_system_spring.application.dto.Lesson;

import com.example.learning_system_spring.domain.model.Role;

public record GetLessonsInput(
    Long courseId,
    Long sectionId,
    Long requesterId,
    Role requesterRole
) {}