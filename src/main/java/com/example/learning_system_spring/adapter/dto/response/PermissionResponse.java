package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.Permission.PermissionOutput;

public record PermissionResponse(Long id, String name, String description) {
    public static PermissionResponse from(PermissionOutput output) {
        return new PermissionResponse(output.id(), output.name(), output.description());
    }
}
