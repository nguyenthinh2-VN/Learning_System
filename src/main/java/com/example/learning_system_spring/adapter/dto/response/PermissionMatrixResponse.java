package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.Permission.PermissionMatrixOutput;

import java.util.List;

public record PermissionMatrixResponse(
        List<PermissionResponse> allPermissions,
        List<RolePermissionsResponse> roles) {

    public static PermissionMatrixResponse from(PermissionMatrixOutput output) {
        return new PermissionMatrixResponse(
                output.allPermissions().stream().map(PermissionResponse::from).toList(),
                output.roles().stream().map(RolePermissionsResponse::from).toList());
    }
}
