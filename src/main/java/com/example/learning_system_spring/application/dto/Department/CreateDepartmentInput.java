package com.example.learning_system_spring.application.dto.Department;

public record CreateDepartmentInput(String code, String name, Long parentId) {
}
