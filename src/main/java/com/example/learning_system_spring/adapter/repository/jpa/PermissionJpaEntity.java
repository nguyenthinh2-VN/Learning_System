package com.example.learning_system_spring.adapter.repository.jpa;

import com.example.learning_system_spring.domain.model.Permission;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PermissionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    public Permission toDomain() {
        return Permission.reconstitute(id, name, description);
    }

    public static PermissionJpaEntity fromDomain(Permission permission) {
        PermissionJpaEntity e = new PermissionJpaEntity();
        e.id = permission.getId();
        e.name = permission.getName();
        e.description = permission.getDescription();
        return e;
    }
}
