package com.example.learning_system_spring.adapter.dto.request.Section;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateSectionRequest {

    @NotBlank(message = "Tiêu đề chương học không được để trống")
    private String title;

    @Min(value = 0, message = "Thứ tự phải >= 0")
    private int orderIndex;
}
