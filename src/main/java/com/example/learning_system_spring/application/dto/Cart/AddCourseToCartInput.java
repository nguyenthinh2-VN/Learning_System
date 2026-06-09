package com.example.learning_system_spring.application.dto.Cart;

import java.util.List;

public record AddCourseToCartInput(
        Long requesterId,
        List<Long> courseIds
) {}
