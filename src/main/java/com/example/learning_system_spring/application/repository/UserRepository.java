package com.example.learning_system_spring.application.repository;

import com.example.learning_system_spring.domain.model.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);
    User save(User user);
    boolean existsByEmail(String email);
}
