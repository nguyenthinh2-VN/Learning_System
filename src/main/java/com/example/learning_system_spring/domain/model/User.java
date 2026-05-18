package com.example.learning_system_spring.domain.model;

import com.example.learning_system_spring.domain.exception.InvalidEmailException;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

public class User {
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private Long id;
    private String email;
    private String password;
    private String name;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private User(Long id, String email, String password, String name, Role role) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
    }

    public static User create(String email, String password, String name, Role role) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidEmailException(email);
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role must not be null");
        }
        User user = new User(null, email.toLowerCase().trim(), password, name.trim(), role);
        user.createdAt = LocalDateTime.now();
        user.updatedAt = LocalDateTime.now();
        return user;
    }

    public static User reconstitute(Long id, String email, String password, String name,
                                     Role role, LocalDateTime createdAt, LocalDateTime updatedAt) {
        User user = new User(id, email, password, name, role);
        user.createdAt = createdAt;
        user.updatedAt = updatedAt;
        return user;
    }

    public boolean passwordMatches(String rawPassword) {
        return this.password.equals(rawPassword);
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getName() { return name; }
    public Role getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
