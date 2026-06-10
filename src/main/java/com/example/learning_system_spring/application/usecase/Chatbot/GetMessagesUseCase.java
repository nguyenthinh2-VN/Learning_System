package com.example.learning_system_spring.application.usecase.Chatbot;

import com.example.learning_system_spring.application.dto.Chatbot.ChatMessageOutput;
import com.example.learning_system_spring.application.repository.Chatbot.ChatMessageRepository;
import com.example.learning_system_spring.application.repository.Chatbot.ChatSessionRepository;
import com.example.learning_system_spring.domain.model.ChatSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetMessagesUseCase {
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional(readOnly = true)
    public List<ChatMessageOutput> execute(Long userId, Long sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Session not found or forbidden"));

        return chatMessageRepository.findBySessionId(sessionId).stream()
                .map(ChatMessageOutput::from)
                .collect(Collectors.toList());
    }
}
