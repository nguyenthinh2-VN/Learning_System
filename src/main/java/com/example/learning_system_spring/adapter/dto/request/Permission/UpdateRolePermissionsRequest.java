package com.example.learning_system_spring.adapter.dto.request.Permission;

import com.example.learning_system_spring.application.dto.Permission.UpdateRolePermissionsInput;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateRolePermissionsRequest {

    /** Danh sách permission mới (replace toàn bộ). Cho phép rỗng để gỡ hết quyền. */
    @NotNull(message = "permissions không được null")
    private List<String> permissions;

    public UpdateRolePermissionsInput toInput(String roleName, Long actorId) {
        return new UpdateRolePermissionsInput(roleName, permissions, actorId);
    }
}
