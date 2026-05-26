package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.User.MyEnrollmentOutput;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MyEnrollmentResponse(
        Long enrollmentId,
        Long courseId,
        BigDecimal paidPrice,
        LocalDateTime enrolledAt
) {
    public static MyEnrollmentResponse from(MyEnrollmentOutput output) {
        return new MyEnrollmentResponse(
                output.enrollmentId(),
                output.courseId(),
                output.paidPrice(),
                output.enrolledAt()
        );
    }
}
