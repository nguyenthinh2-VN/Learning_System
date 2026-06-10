package com.example.learning_system_spring.adapter.repository.jpa.ChatbotEntity;

import com.example.learning_system_spring.adapter.repository.jpa.UserEntity.UserJpaEntity;
import com.example.learning_system_spring.domain.model.ChatSession;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_sessions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSessionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

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

    public ChatSession toDomain() {
        return ChatSession.builder()
                .id(id)
                .userId(user.getId())
                .title(title)
                .summary(summary)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public static ChatSessionJpaEntity fromDomain(ChatSession session, UserJpaEntity userEntity) {
        ChatSessionJpaEntity e = new ChatSessionJpaEntity();
        e.setId(session.getId());
        e.setUser(userEntity);
        e.setTitle(session.getTitle());
        e.setSummary(session.getSummary());
        e.setCreatedAt(session.getCreatedAt());
        e.setUpdatedAt(session.getUpdatedAt());
        return e;
    }
}
