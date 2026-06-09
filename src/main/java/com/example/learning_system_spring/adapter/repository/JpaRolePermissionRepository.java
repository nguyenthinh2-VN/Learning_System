package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.role_permissionEntity.RolePermissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface JpaRolePermissionRepository extends JpaRepository<RolePermissionJpaEntity, Long> {

    List<RolePermissionJpaEntity> findByRole_Name(String roleName);

    void deleteByRole_Id(Long roleId);
}
