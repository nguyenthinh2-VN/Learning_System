package com.example.learning_system_spring.application.dto.User;

import com.example.learning_system_spring.domain.model.Enrollment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MyEnrollmentOutput(
        Long enrollmentId,
        Long courseId,
        BigDecimal paidPrice,
        LocalDateTime enrolledAt
) {
    public static MyEnrollmentOutput from(Enrollment e) {
        return new MyEnrollmentOutput(e.getId(), e.getCourseId(), e.getPaidPrice(), e.getEnrolledAt());
    }
}
