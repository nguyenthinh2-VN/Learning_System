package com.example.learning_system_spring.application.repository.User;

import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.domain.model.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);
    Optional<User> findById(Long id);
    Optional<User> findByIdForUpdate(Long id);
    User save(User user);
    boolean existsByEmail(String email);
    PageResult<User> findAll(String keyword, int page, int size);
}
