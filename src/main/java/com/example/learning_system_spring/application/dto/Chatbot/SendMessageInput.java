package com.example.learning_system_spring.application.dto.Chatbot;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SendMessageInput {
    private Long userId;
    private Long sessionId;
    private String content;
}
