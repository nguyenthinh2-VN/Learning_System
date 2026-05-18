package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface JpaUserRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByEmail(String email);
    Optional<UserJpaEntity> findByUsernameOrEmail(String username, String email);
    boolean existsByEmail(String email);
}
