package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.role_permissionEntity.RoleJpaEntity;
import com.example.learning_system_spring.adapter.repository.jpa.UserEntity.UserJpaEntity;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.domain.model.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final JpaUserRepository jpaRepo;
    private final EntityManager em;

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepo.findByEmail(email).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByUsernameOrEmail(String username, String email) {
        return jpaRepo.findByUsernameOrEmail(username, email).map(UserJpaEntity::toDomain);
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = UserJpaEntity.fromDomain(user);
        RoleJpaEntity managedRole = em.getReference(RoleJpaEntity.class, user.getRole().getId());
        entity.setRole(managedRole);
        UserJpaEntity saved = jpaRepo.save(entity);
        return saved.toDomain();
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepo.existsByEmail(email);
    }

    @Override
    public Optional<User> findByIdForUpdate(Long id) {
        return jpaRepo.findByIdForUpdate(id).map(UserJpaEntity::toDomain);
    }
}
