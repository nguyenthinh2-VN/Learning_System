package com.example.learning_system_spring.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Course {
    private Long id;
    private String title;
    private String description;
    private int maxStudents;
    private int enrolledCount;
    private BigDecimal price;
    private Long instructorId;
    @Builder.Default
    private List<CourseSection> sections = new ArrayList<>();

    // Use builder for creation, but can add factory methods if needed
    public static Course create(String title, String description, int maxStudents, BigDecimal price, Long instructorId,
            List<CourseSection> sections) {
        if (maxStudents <= 0) {
            throw new IllegalArgumentException("Max students must be greater than 0");
        }
        return Course.builder()
                .title(title)
                .description(description)
                .maxStudents(maxStudents)
                .price(price != null ? price : BigDecimal.ZERO)
                .enrolledCount(0)
                .instructorId(instructorId)
                .sections(sections != null ? new ArrayList<>(sections) : new ArrayList<>())
                .build();
    }

    public static Course reconstitute(Long id, String title, String description, int maxStudents, int enrolledCount,
            BigDecimal price, Long instructorId, List<CourseSection> sections) {
        return Course.builder()
                .id(id)
                .title(title)
                .description(description)
                .maxStudents(maxStudents)
                .price(price != null ? price : BigDecimal.ZERO)
                .enrolledCount(enrolledCount)
                .instructorId(instructorId)
                .sections(sections != null ? new ArrayList<>(sections) : new ArrayList<>())
                .build();
    }

    public boolean isFull() {
        return enrolledCount >= maxStudents;
    }

    public void enroll() {
        if (isFull()) {
            throw new IllegalStateException("Course is already full");
        }
        enrolledCount++;
    }
}
