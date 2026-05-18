package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.role_permissionEntity.RoleJpaEntity;
import com.example.learning_system_spring.application.repository.RoleRepository;
import com.example.learning_system_spring.domain.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {
    private final JpaRoleRepository jpaRepo;

    @Override
    public Optional<Role> findByName(String name) {
        return jpaRepo.findByName(name).map(RoleJpaEntity::toDomain);
    }
}
