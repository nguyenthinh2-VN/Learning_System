package com.example.learning_system_spring.application.repository.Chatbot;

import com.example.learning_system_spring.domain.model.ChatSession;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository {
    ChatSession save(ChatSession session);
    Optional<ChatSession> findById(Long id);
    List<ChatSession> findByUserId(Long userId);
    void deleteById(Long id);
}
