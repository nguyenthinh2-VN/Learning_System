package com.example.learning_system_spring.domain.model;

import lombok.Getter;

import java.util.Objects;
@Getter
public class Permission {
    private Long id;
    private String name;
    private String description;

    private Permission(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public static Permission create(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Permission name must not be blank");
        }
        return new Permission(null, name.toUpperCase().trim(), description);
    }

    public static Permission reconstitute(Long id, String name, String description) {
        return new Permission(id, name, description);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
