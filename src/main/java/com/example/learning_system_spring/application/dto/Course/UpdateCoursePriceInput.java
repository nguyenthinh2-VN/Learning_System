package com.example.learning_system_spring.application.dto.Course;

import com.example.learning_system_spring.domain.model.Role;

import java.math.BigDecimal;

public record UpdateCoursePriceInput(
        Long courseId,
        Long requesterId,
        Role requesterRole,
        BigDecimal newPrice
) {
}
