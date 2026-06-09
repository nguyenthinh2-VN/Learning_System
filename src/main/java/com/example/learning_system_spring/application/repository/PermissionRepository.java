package com.example.learning_system_spring.application.repository;

import com.example.learning_system_spring.domain.model.Permission;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository {
    List<Permission> findAll();

    Optional<Permission> findByName(String name);

    boolean existsByName(String name);
}
