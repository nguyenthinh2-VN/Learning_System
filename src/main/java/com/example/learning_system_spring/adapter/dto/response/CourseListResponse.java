package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.Course.GetCourseListOutput;
import com.example.learning_system_spring.application.dto.PageResult;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Builder
public class CourseListResponse {
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
    private List<CourseItem> items;

    @Getter
    @Builder
    public static class CourseItem {
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
        private boolean freeForInternal;
        @JsonProperty("isMandatory")
        private boolean isMandatory;
        private String assignedDepartment;
        private LocalDateTime mandatoryDeadline;
        private LocalDateTime publishedAt;
    }

    public static CourseListResponse from(PageResult<GetCourseListOutput> pageResult) {
        List<CourseItem> items = pageResult.items().stream()
                .map(output -> CourseItem.builder()
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
                        .freeForInternal(output.freeForInternal())
                        .isMandatory(output.isMandatory())
                        .assignedDepartment(output.assignedDepartment())
                        .mandatoryDeadline(output.mandatoryDeadline())
                        .publishedAt(output.publishedAt())
                        .build())
                .toList();

        return CourseListResponse.builder()
                .totalElements(pageResult.totalElements())
                .totalPages(pageResult.totalPages())
                .page(pageResult.page())
                .size(pageResult.size())
                .items(items)
                .build();
    }
}
