package com.example.learning_system_spring.domain.model;

import java.util.Objects;

public class Role {
    private Long id;
    private String name;
    private String description;

    private Role(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public static Role create(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Role name must not be blank");
        }
        return new Role(null, name.toUpperCase().trim(), description);
    }

    public static Role reconstitute(Long id, String name, String description) {
        return new Role(id, name, description);
    }

    public boolean isMember() { return "MEMBER".equals(name); }
    public boolean isStaff() { return "STAFF".equals(name); }
    public boolean isAdmin() { return "ADMIN".equals(name); }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role role)) return false;
        return Objects.equals(id, role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
