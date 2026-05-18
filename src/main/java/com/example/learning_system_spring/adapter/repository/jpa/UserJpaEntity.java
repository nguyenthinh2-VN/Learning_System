package com.example.learning_system_spring.adapter.repository.jpa;

import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.model.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleJpaEntity role;

    @Column(name = "is_internal", nullable = false)
    private boolean isInternal;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public User toDomain() {
        return User.reconstitute(id, username, email, password, name, role.toDomain(), isInternal, createdAt, updatedAt);
    }

    public static UserJpaEntity fromDomain(User user) {
        UserJpaEntity e = new UserJpaEntity();
        e.id = user.getId();
        e.username = user.getUsername();
        e.email = user.getEmail();
        e.password = user.getPassword();
        e.name = user.getName();
        e.role = RoleJpaEntity.fromDomain(user.getRole());
        e.isInternal = user.isInternal();
        e.createdAt = user.getCreatedAt();
        e.updatedAt = user.getUpdatedAt();
        return e;
    }
}
