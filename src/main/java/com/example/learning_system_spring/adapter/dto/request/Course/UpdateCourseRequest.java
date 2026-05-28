package com.example.learning_system_spring.adapter.dto.request.Course;

import com.example.learning_system_spring.application.dto.Course.CourseLessonDto;
import com.example.learning_system_spring.application.dto.Course.CourseSectionDto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCourseRequest {
    @NotBlank
    private String title;

    private String description;

    @Min(1)
    private int maxStudents;

    @Min(0)
    private BigDecimal price;

    private String thumbnailUrl;

    private List<CourseSectionDto> sections;
}
