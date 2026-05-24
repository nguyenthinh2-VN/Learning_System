package com.example.learning_system_spring.adapter.dto.request.Course;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCoursePriceRequest {

    @NotNull(message = "Giá khóa học không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Giá khóa học phải >= 0")
    private BigDecimal price;
}
