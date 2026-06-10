package com.example.learning_system_spring.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private Long id;
    private Long sessionId;
    private ChatRole role;
    private String content;
    private String recommendedCourses; // Stored as JSON string
    private LocalDateTime createdAt;

    public static ChatMessage create(Long sessionId, ChatRole role, String content, String recommendedCourses) {
        return ChatMessage.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .recommendedCourses(recommendedCourses)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
