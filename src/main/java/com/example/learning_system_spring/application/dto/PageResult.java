package com.example.learning_system_spring.application.dto;

import java.util.List;

public record PageResult<T>(
        long totalElements,
        int totalPages,
        int page,
        int size,
        List<T> items
) {
    public static <T> PageResult<T> of(long totalElements, int totalPages, int page, int size, List<T> items) {
        return new PageResult<>(totalElements, totalPages, page, size, items);
    }
}
