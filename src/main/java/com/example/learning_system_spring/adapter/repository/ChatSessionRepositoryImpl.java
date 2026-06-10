package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.ChatbotEntity.ChatSessionJpaEntity;
import com.example.learning_system_spring.adapter.repository.jpa.UserEntity.UserJpaEntity;
import com.example.learning_system_spring.application.repository.Chatbot.ChatSessionRepository;
import com.example.learning_system_spring.adapter.repository.JpaUserRepository;
import com.example.learning_system_spring.domain.model.ChatSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ChatSessionRepositoryImpl implements ChatSessionRepository {

    private final JpaChatSessionRepository jpaRepository;
    private final JpaUserRepository userRepository;

    @Override
    public ChatSession save(ChatSession session) {
        UserJpaEntity user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        ChatSessionJpaEntity entity = ChatSessionJpaEntity.fromDomain(session, user);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<ChatSession> findById(Long id) {
        return jpaRepository.findById(id).map(ChatSessionJpaEntity::toDomain);
    }

    @Override
    public List<ChatSession> findByUserId(Long userId) {
        return jpaRepository.findByUser_IdOrderByUpdatedAtDesc(userId).stream()
                .map(ChatSessionJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
