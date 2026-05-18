package com.example.learning_system_spring.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseSection {
    private Long id;
    private String title;
    private int orderIndex;
    @Builder.Default
    private List<CourseLesson> lessons = new ArrayList<>();

    public static CourseSection create(String title, int orderIndex, List<CourseLesson> lessons) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Section title cannot be blank");
        }
        return CourseSection.builder()
                .title(title)
                .orderIndex(orderIndex)
                .lessons(lessons != null ? new ArrayList<>(lessons) : new ArrayList<>())
                .build();
    }

    public static CourseSection reconstitute(Long id, String title, int orderIndex, List<CourseLesson> lessons) {
        return CourseSection.builder()
                .id(id)
                .title(title)
                .orderIndex(orderIndex)
                .lessons(lessons != null ? new ArrayList<>(lessons) : new ArrayList<>())
                .build();
    }
}
