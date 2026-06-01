package com.example.learning_system_spring.application.usecase.Permission;

import com.example.learning_system_spring.application.dto.Permission.RolePermissionsOutput;
import com.example.learning_system_spring.application.repository.RoleRepository;
import com.example.learning_system_spring.application.repository.RolePermissionRepository;
import com.example.learning_system_spring.domain.exception.RoleNotFoundException;
import com.example.learning_system_spring.domain.service.RolePermissionPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetRolePermissionsUseCase {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Transactional(readOnly = true)
    public RolePermissionsOutput execute(String roleName) {
        roleRepository.findByName(roleName)
                .orElseThrow(() -> new RoleNotFoundException(roleName));

        var rolePermissions = rolePermissionRepository.findByRoleName(roleName);
        return RolePermissionsOutput.from(rolePermissions, RolePermissionPolicy.isProtected(roleName));
    }
}
