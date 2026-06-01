package com.example.learning_system_spring.application.usecase.Permission;

import com.example.learning_system_spring.application.dto.Permission.PermissionMatrixOutput;
import com.example.learning_system_spring.application.dto.Permission.PermissionOutput;
import com.example.learning_system_spring.application.dto.Permission.RolePermissionsOutput;
import com.example.learning_system_spring.application.repository.PermissionRepository;
import com.example.learning_system_spring.application.repository.RolePermissionRepository;
import com.example.learning_system_spring.domain.service.RolePermissionPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetPermissionMatrixUseCase {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Transactional(readOnly = true)
    public PermissionMatrixOutput execute() {
        List<PermissionOutput> allPermissions = permissionRepository.findAll().stream()
                .map(PermissionOutput::from)
                .toList();

        List<RolePermissionsOutput> roles = rolePermissionRepository.findMatrix().stream()
                .map(rp -> RolePermissionsOutput.from(rp, RolePermissionPolicy.isProtected(rp.getRoleName())))
                .toList();

        return new PermissionMatrixOutput(allPermissions, roles);
    }
}
