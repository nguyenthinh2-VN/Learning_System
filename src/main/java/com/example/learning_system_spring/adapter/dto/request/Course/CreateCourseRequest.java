package com.example.learning_system_spring.adapter.dto.request.Course;

import com.example.learning_system_spring.application.dto.Course.CourseSectionDto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@NoArgsConstructor
public class CreateCourseRequest {
    @NotBlank
    private String title;

    private String description;

    @Min(1)
    private int maxStudents;

    @Min(0)
    private BigDecimal price;

    private String thumbnailUrl;

    private Long requestedInstructorId;

    /** Miễn phí cho thành viên nội bộ (user.isInternal). Mặc định false. */
    private boolean freeForInternal;

    @JsonProperty("isMandatory")
    private boolean isMandatory;
    private String assignedDepartment;
    private java.time.LocalDateTime mandatoryDeadline;

    private List<CourseSectionDto> sections;
}
