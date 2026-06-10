package com.example.learning_system_spring.adapter.repository.jpa.ChatbotEntity;

import com.example.learning_system_spring.domain.model.ChatMessage;
import com.example.learning_system_spring.domain.model.ChatRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSessionJpaEntity session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "recommended_courses", columnDefinition = "TEXT")
    private String recommendedCourses;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public ChatMessage toDomain() {
        return ChatMessage.builder()
                .id(id)
                .sessionId(session.getId())
                .role(role)
                .content(content)
                .recommendedCourses(recommendedCourses)
                .createdAt(createdAt)
                .build();
    }

    public static ChatMessageJpaEntity fromDomain(ChatMessage message, ChatSessionJpaEntity sessionEntity) {
        ChatMessageJpaEntity e = new ChatMessageJpaEntity();
        e.setId(message.getId());
        e.setSession(sessionEntity);
        e.setRole(message.getRole());
        e.setContent(message.getContent());
        e.setRecommendedCourses(message.getRecommendedCourses());
        e.setCreatedAt(message.getCreatedAt());
        return e;
    }
}
