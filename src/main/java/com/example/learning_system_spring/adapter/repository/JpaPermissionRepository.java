package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.role_permissionEntity.PermissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface JpaPermissionRepository extends JpaRepository<PermissionJpaEntity, Long> {
    Optional<PermissionJpaEntity> findByName(String name);

    boolean existsByName(String name);
}
