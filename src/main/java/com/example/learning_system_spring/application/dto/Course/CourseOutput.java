package com.example.learning_system_spring.application.dto.Course;

import com.example.learning_system_spring.domain.model.Course;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CourseOutput(
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
                LocalDateTime publishedAt,
                Long publishedBy,
                List<CourseSectionDto> sections) {
        public static CourseOutput from(Course course) {
                if (course == null)
                        return null;

                List<CourseSectionDto> sectionDtos = course.getSections().stream()
                                .map(s -> new CourseSectionDto(
                                                s.getTitle(),
                                                s.getOrderIndex(),
                                                s.getLessons().stream()
                                                                .map(l -> new CourseLessonDto(l.getTitle(),
                                                                                l.getContentUrl(), l.getOrderIndex()))
                                                                .collect(Collectors.toList())))
                                .collect(Collectors.toList());

                return new CourseOutput(
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
                                course.getPublishedAt(),
                                course.getPublishedBy(),
                                sectionDtos);
        }
}
