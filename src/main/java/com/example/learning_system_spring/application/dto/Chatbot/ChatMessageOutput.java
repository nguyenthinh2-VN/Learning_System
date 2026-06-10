package com.example.learning_system_spring.application.dto.Chatbot;

import com.example.learning_system_spring.domain.model.ChatMessage;
import com.example.learning_system_spring.domain.model.ChatRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageOutput {
    private Long id;
    private ChatRole role;
    private String content;
    private String recommendedCourses;
    private LocalDateTime createdAt;

    public static ChatMessageOutput from(ChatMessage message) {
        return ChatMessageOutput.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .recommendedCourses(message.getRecommendedCourses())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
