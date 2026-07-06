package com.example.learning_system_spring.application.dto.Department;

public record UpdateDepartmentInput(
        Long id,
        String code,
        String name,
        Long parentId
) {
}
