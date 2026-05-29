package com.example.learning_system_spring.domain.model;

import com.example.learning_system_spring.domain.exception.InvalidEmailException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

public class User {
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private Long id;
    private String username;
    private String email;
    private String password;
    private String name;
    private Role role;
    private boolean isInternal;
    private BigDecimal balance;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private User(Long id, String username, String email, String password, String name, Role role, boolean isInternal, BigDecimal balance) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
        this.isInternal = isInternal;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
    }

    public static User create(String username, String email, String password, String name, Role role, boolean isInternal) {
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
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        User user = new User(null, username.trim(), email.toLowerCase().trim(), password, name.trim(), role, isInternal, BigDecimal.ZERO);
        user.createdAt = LocalDateTime.now();
        user.updatedAt = LocalDateTime.now();
        return user;
    }

    public static User reconstitute(Long id, String username, String email, String password, String name,
                                     Role role, boolean isInternal, BigDecimal balance, LocalDateTime createdAt, LocalDateTime updatedAt) {
        User user = new User(id, username, email, password, name, role, isInternal, balance);
        user.createdAt = createdAt;
        user.updatedAt = updatedAt;
        return user;
    }

    /**
     * Overload có avatarUrl — dùng khi tái dựng từ DB (UserJpaEntity).
     * Overload cũ (không avatarUrl) giữ nguyên cho code/test hiện hữu; avatarUrl mặc định null.
     */
    public static User reconstitute(Long id, String username, String email, String password, String name,
                                     Role role, boolean isInternal, BigDecimal balance, String avatarUrl,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        User user = reconstitute(id, username, email, password, name, role, isInternal, balance, createdAt, updatedAt);
        user.avatarUrl = avatarUrl;
        return user;
    }

    public boolean passwordMatches(String rawPassword) {
        return this.password.equals(rawPassword);
    }

    /**
     * Đổi họ tên hiển thị. Tên rỗng/trắng bị từ chối.
     */
    public void changeName(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }
        this.name = newName.trim();
    }

    /**
     * Cập nhật avatar.
     * - null  = giữ nguyên (no-op).
     * - ""    = xóa avatar (về null).
     * - chuỗi = set giá trị mới (đã trim).
     */
    public void changeAvatar(String newAvatarUrl) {
        if (newAvatarUrl != null) {
            this.avatarUrl = newAvatarUrl.isBlank() ? null : newAvatarUrl.trim();
        }
    }

    /**
     * Đặt lại mật khẩu. CHỈ nhận mật khẩu ĐÃ được encode ở tầng application.
     * Domain không biết tới thuật toán hash (PasswordEncoder là hạ tầng).
     */
    public void changePassword(String newEncodedPassword) {
        if (newEncodedPassword == null || newEncodedPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        this.password = newEncodedPassword;
    }

    public void addBalance(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Top-up amount must be greater than zero");
        }
        this.balance = this.balance.add(amount);
    }

    public void deductBalance(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Deduct amount must not be negative");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
        this.balance = this.balance.subtract(amount);
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getName() { return name; }
    public Role getRole() { return role; }
    public boolean isInternal() { return isInternal; }
    public BigDecimal getBalance() { return balance; }
    public String getAvatarUrl() { return avatarUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
