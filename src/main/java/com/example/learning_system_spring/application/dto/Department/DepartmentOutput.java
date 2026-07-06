package com.example.learning_system_spring.application.dto.Department;

public record DepartmentOutput(
        Long id,
        String code,
        String name,
        Long parentId,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt
) {
    public static DepartmentOutput fromDomain(com.example.learning_system_spring.domain.model.Department department) {
        return new DepartmentOutput(
                department.getId(),
                department.getCode(),
                department.getName(),
                department.getParentId(),
                department.getCreatedAt(),
                department.getUpdatedAt()
        );
    }
}
