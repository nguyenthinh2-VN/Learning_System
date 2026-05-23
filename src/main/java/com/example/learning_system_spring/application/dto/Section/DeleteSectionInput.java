package com.example.learning_system_spring.application.dto.Section;

import com.example.learning_system_spring.domain.model.Role;

public record DeleteSectionInput(
        Long sectionId,
        Long courseId,
        Long requesterId,
        Role requesterRole) {
}
