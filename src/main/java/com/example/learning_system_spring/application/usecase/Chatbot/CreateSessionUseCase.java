package com.example.learning_system_spring.application.usecase.Chatbot;

import com.example.learning_system_spring.application.dto.Chatbot.CreateSessionOutput;
import com.example.learning_system_spring.application.repository.Chatbot.ChatSessionRepository;
import com.example.learning_system_spring.domain.model.ChatSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateSessionUseCase {
    private final ChatSessionRepository chatSessionRepository;

    @Transactional
    public CreateSessionOutput execute(Long userId) {
        ChatSession session = ChatSession.create(userId, "New Conversation");
        ChatSession saved = chatSessionRepository.save(session);
        return CreateSessionOutput.from(saved);
    }
}
