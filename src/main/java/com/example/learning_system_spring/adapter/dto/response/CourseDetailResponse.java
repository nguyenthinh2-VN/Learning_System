package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.Course.CourseOutput;
import com.example.learning_system_spring.application.dto.Course.CourseSectionDto;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class CourseDetailResponse {
    private Long id;
    private String title;
    private String description;
    private int maxStudents;
    private int enrolledCount;
    private BigDecimal price;
    private Long instructorId;
    private String thumbnailUrl;
    private boolean published;
    private boolean priceLocked;
    private LocalDateTime publishedAt;
    private Long publishedBy;
    private java.util.List<CourseSectionDto> sections;

    public static CourseDetailResponse from(CourseOutput output) {
        if (output == null)
            return null;
        return CourseDetailResponse.builder()
                .id(output.id())
                .title(output.title())
                .description(output.description())
                .maxStudents(output.maxStudents())
                .enrolledCount(output.enrolledCount())
                .price(output.price())
                .instructorId(output.instructorId())
                .thumbnailUrl(output.thumbnailUrl())
                .published(output.published())
                .priceLocked(output.priceLocked())
                .publishedAt(output.publishedAt())
                .publishedBy(output.publishedBy())
                .sections(output.sections())
                .build();
    }
}
