package com.example.learning_system_spring.application.repository;

import com.example.learning_system_spring.domain.model.Role;

import java.util.Optional;

public interface RoleRepository {
    Optional<Role> findByName(String name);
}
