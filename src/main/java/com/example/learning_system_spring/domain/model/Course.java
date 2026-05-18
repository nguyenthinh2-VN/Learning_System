package com.example.learning_system_spring.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Course {
    private Long id;
    private String title;
    private String description;
    private int maxStudents;
    private int enrolledCount;

    // Use builder for creation, but can add factory methods if needed
    public static Course create(String title, String description, int maxStudents) {
        if (maxStudents <= 0) {
            throw new IllegalArgumentException("Max students must be greater than 0");
        }
        return Course.builder()
                .title(title)
                .description(description)
                .maxStudents(maxStudents)
                .enrolledCount(0)
                .build();
    }

    public static Course reconstitute(Long id, String title, String description, int maxStudents, int enrolledCount) {
        return Course.builder()
                .id(id)
                .title(title)
                .description(description)
                .maxStudents(maxStudents)
                .enrolledCount(enrolledCount)
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
