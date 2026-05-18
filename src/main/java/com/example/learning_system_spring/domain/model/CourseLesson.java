package com.example.learning_system_spring.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseLesson {
    private Long id;
    private String title;
    private String contentUrl;
    private int orderIndex;

    public static CourseLesson create(String title, String contentUrl, int orderIndex) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Lesson title cannot be blank");
        }
        return CourseLesson.builder()
                .title(title)
                .contentUrl(contentUrl)
                .orderIndex(orderIndex)
                .build();
    }

    public static CourseLesson reconstitute(Long id, String title, String contentUrl, int orderIndex) {
        return CourseLesson.builder()
                .id(id)
                .title(title)
                .contentUrl(contentUrl)
                .orderIndex(orderIndex)
                .build();
    }
}
