package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.ChatbotEntity.ChatMessageJpaEntity;
import com.example.learning_system_spring.adapter.repository.jpa.ChatbotEntity.ChatSessionJpaEntity;
import com.example.learning_system_spring.application.repository.Chatbot.ChatMessageRepository;
import com.example.learning_system_spring.domain.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepository {

    private final JpaChatMessageRepository jpaRepository;
    private final JpaChatSessionRepository sessionRepository;

    @Override
    public ChatMessage save(ChatMessage message) {
        ChatSessionJpaEntity session = sessionRepository.findById(message.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        ChatMessageJpaEntity entity = ChatMessageJpaEntity.fromDomain(message, session);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public List<ChatMessage> findBySessionId(Long sessionId) {
        return jpaRepository.findBySession_IdOrderByCreatedAtAsc(sessionId).stream()
                .map(ChatMessageJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countBySessionId(Long sessionId) {
        return jpaRepository.countBySession_Id(sessionId);
    }
}
