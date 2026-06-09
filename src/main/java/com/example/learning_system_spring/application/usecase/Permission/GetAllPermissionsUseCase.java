package com.example.learning_system_spring.application.usecase.Permission;

import com.example.learning_system_spring.application.dto.Permission.PermissionOutput;
import com.example.learning_system_spring.application.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAllPermissionsUseCase {

    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<PermissionOutput> execute() {
        return permissionRepository.findAll().stream()
                .map(PermissionOutput::from)
                .toList();
    }
}
