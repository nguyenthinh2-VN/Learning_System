package com.example.learning_system_spring.domain.model;

import java.time.LocalDateTime;

public class Department {
    private Long id;
    private String code;
    private String name;
    private Long parentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Department(String code, String name, Long parentId) {
        this.code = code;
        this.name = name;
        this.parentId = parentId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    private Department() {
    }

    public static Department reconstitute(Long id, String code, String name, Long parentId,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        Department dept = new Department();
        dept.id = id;
        dept.code = code;
        dept.name = name;
        dept.parentId = parentId;
        dept.createdAt = createdAt;
        dept.updatedAt = updatedAt;
        return dept;
    }

    public void update(String code, String name, Long parentId) {
        this.code = code;
        this.name = name;
        this.parentId = parentId;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Long getParentId() {
        return parentId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
