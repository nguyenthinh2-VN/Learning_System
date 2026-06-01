package com.example.learning_system_spring.application.dto.Permission;

import com.example.learning_system_spring.domain.model.Permission;

public record PermissionOutput(Long id, String name, String description) {
    public static PermissionOutput from(Permission permission) {
        return new PermissionOutput(permission.getId(), permission.getName(), permission.getDescription());
    }
}
