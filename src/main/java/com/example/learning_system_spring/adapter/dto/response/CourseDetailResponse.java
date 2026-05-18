package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.GetCourseDetailOutput;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CourseDetailResponse {
    private Long id;
    private String title;
    private String description;
    private int maxStudents;
    private int enrolledCount;

    public static CourseDetailResponse from(GetCourseDetailOutput output) {
        return CourseDetailResponse.builder()
                .id(output.id())
                .title(output.title())
                .description(output.description())
                .maxStudents(output.maxStudents())
                .enrolledCount(output.enrolledCount())
                .build();
    }
}
