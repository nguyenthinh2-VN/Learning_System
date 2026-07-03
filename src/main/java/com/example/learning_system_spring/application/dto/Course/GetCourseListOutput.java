package com.example.learning_system_spring.application.dto.Course;

import com.example.learning_system_spring.domain.model.Course;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

public record GetCourseListOutput(
        Long id,
        String title,
        String description,
        int maxStudents,
        int enrolledCount,
        BigDecimal price,
        Long instructorId,
        String thumbnailUrl,
        boolean published,
        boolean priceLocked,
        boolean freeForInternal,
        @JsonProperty("isMandatory") boolean isMandatory,
        String assignedDepartment,
        LocalDateTime mandatoryDeadline,
        LocalDateTime publishedAt) {
    public static GetCourseListOutput from(Course course) {
        return new GetCourseListOutput(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getMaxStudents(),
                course.getEnrolledCount(),
                course.getPrice(),
                course.getInstructorId(),
                course.getThumbnailUrl(),
                course.isPublished(),
                course.isPriceLocked(),
                course.isFreeForInternal(),
                course.isMandatory(),
                course.getAssignedDepartment(),
                course.getMandatoryDeadline(),
                course.getPublishedAt());
    }
}
