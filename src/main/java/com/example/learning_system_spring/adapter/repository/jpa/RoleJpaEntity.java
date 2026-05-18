package com.example.learning_system_spring.adapter.repository.jpa;

import com.example.learning_system_spring.domain.model.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    public Role toDomain() {
        return Role.reconstitute(id, name, description);
    }

    public static RoleJpaEntity fromDomain(Role role) {
        RoleJpaEntity e = new RoleJpaEntity();
        e.id = role.getId();
        e.name = role.getName();
        e.description = role.getDescription();
        return e;
    }
}
