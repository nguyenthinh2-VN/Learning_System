package com.example.learning_system_spring.application.usecase.Permission;

import com.example.learning_system_spring.application.dto.Permission.RolePermissionsOutput;
import com.example.learning_system_spring.application.dto.Permission.UpdateRolePermissionsInput;
import com.example.learning_system_spring.application.repository.PermissionRepository;
import com.example.learning_system_spring.application.repository.RoleRepository;
import com.example.learning_system_spring.application.repository.RolePermissionRepository;
import com.example.learning_system_spring.domain.exception.PermissionNotFoundException;
import com.example.learning_system_spring.domain.exception.RoleNotFoundException;
import com.example.learning_system_spring.domain.model.RolePermissions;
import com.example.learning_system_spring.domain.service.RolePermissionPolicy;
import com.example.learning_system_spring.infrastructure.config.PermissionCacheService;
import com.example.learning_system_spring.infrastructure.service.PermissionAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Thay thế toàn bộ permission của một role (replace-all). Áp luật bất biến
 * ({@link RolePermissionPolicy}: không sửa SUPER_ADMIN), validate permission tồn tại,
 * ghi audit và reload cache để có hiệu lực tức thì.
 */
@Service
@RequiredArgsConstructor
public class UpdateRolePermissionsUseCase {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionCacheService permissionCacheService;
    private final PermissionAuditService permissionAuditService;

    @Transactional
    public RolePermissionsOutput execute(UpdateRolePermissionsInput input) {
        String roleName = input.roleName() == null ? "" : input.roleName().trim().toUpperCase();

        // Role phải tồn tại.
        roleRepository.findByName(roleName)
                .orElseThrow(() -> new RoleNotFoundException(roleName));

        // Không cho sửa ma trận của role được bảo vệ (SUPER_ADMIN luôn full quyền).
        RolePermissionPolicy.assertEditable(roleName);

        // Chuẩn hóa + khử trùng lặp danh sách permission mới.
        Set<String> requested = new LinkedHashSet<>();
        if (input.permissionNames() != null) {
            input.permissionNames().stream()
                    .filter(p -> p != null && !p.isBlank())
                    .map(p -> p.trim().toUpperCase())
                    .forEach(requested::add);
        }

        // Mọi permission yêu cầu phải tồn tại trong hệ thống.
        for (String permName : requested) {
            if (!permissionRepository.existsByName(permName)) {
                throw new PermissionNotFoundException(permName);
            }
        }

        // Trạng thái trước (để ghi audit diff).
        List<String> before = new ArrayList<>(rolePermissionRepository.findByRoleName(roleName).getPermissionNames());

        // Replace-all.
        List<String> after = new ArrayList<>(requested);
        rolePermissionRepository.replacePermissions(roleName, after);

        // Audit diff.
        List<String> granted = after.stream().filter(p -> !before.contains(p)).toList();
        List<String> revoked = before.stream().filter(p -> !after.contains(p)).toList();
        permissionAuditService.logRolePermissionsUpdated(
                input.actorId(), roleName, before, after, granted, revoked);

        // Hiệu lực tức thì.
        permissionCacheService.reload();

        RolePermissions updated = RolePermissions.of(roleName, requested);
        return RolePermissionsOutput.from(updated, RolePermissionPolicy.isProtected(roleName));
    }
}
