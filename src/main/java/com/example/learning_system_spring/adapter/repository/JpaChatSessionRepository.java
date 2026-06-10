package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.ChatbotEntity.ChatSessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaChatSessionRepository extends JpaRepository<ChatSessionJpaEntity, Long> {
    List<ChatSessionJpaEntity> findByUser_IdOrderByUpdatedAtDesc(Long userId);
}
