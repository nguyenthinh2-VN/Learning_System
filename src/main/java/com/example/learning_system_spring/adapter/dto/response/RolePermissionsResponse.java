package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.Permission.RolePermissionsOutput;

import java.util.List;

public record RolePermissionsResponse(String roleName, List<String> permissions, boolean locked) {
    public static RolePermissionsResponse from(RolePermissionsOutput output) {
        return new RolePermissionsResponse(output.roleName(), output.permissions(), output.locked());
    }
}
