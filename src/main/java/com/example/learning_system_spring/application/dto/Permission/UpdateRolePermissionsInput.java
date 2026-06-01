package com.example.learning_system_spring.application.dto.Permission;

import java.util.List;

/**
 * Input cho thao tác replace-all permission của một role.
 *
 * @param roleName        role bị sửa (theo path variable)
 * @param permissionNames danh sách permission mới (thay thế toàn bộ)
 * @param actorId         user thực hiện (để ghi audit)
 */
public record UpdateRolePermissionsInput(
        String roleName,
        List<String> permissionNames,
        Long actorId) {
}
