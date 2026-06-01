package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.role_permissionEntity.PermissionJpaEntity;
import com.example.learning_system_spring.application.repository.PermissionRepository;
import com.example.learning_system_spring.domain.model.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PermissionRepositoryImpl implements PermissionRepository {

    private final JpaPermissionRepository jpaRepo;

    @Override
    public List<Permission> findAll() {
        return jpaRepo.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(PermissionJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Permission> findByName(String name) {
        return jpaRepo.findByName(name).map(PermissionJpaEntity::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepo.existsByName(name);
    }
}
