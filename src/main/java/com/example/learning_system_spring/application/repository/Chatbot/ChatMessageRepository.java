package com.example.learning_system_spring.application.repository.Chatbot;

import com.example.learning_system_spring.domain.model.ChatMessage;

import java.util.List;

public interface ChatMessageRepository {
    ChatMessage save(ChatMessage message);
    List<ChatMessage> findBySessionId(Long sessionId);
    long countBySessionId(Long sessionId);
}
