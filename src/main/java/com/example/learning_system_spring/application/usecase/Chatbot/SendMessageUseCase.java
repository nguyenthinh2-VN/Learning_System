package com.example.learning_system_spring.application.usecase.Chatbot;

import com.example.learning_system_spring.application.dto.Chatbot.SendMessageInput;
import com.example.learning_system_spring.application.dto.Chatbot.SendMessageOutput;
import com.example.learning_system_spring.application.port.LlmProvider;
import com.example.learning_system_spring.application.repository.Chatbot.ChatMessageRepository;
import com.example.learning_system_spring.application.repository.Chatbot.ChatSessionRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.domain.model.ChatMessage;
import com.example.learning_system_spring.domain.model.ChatRole;
import com.example.learning_system_spring.domain.model.ChatSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SendMessageUseCase {
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CourseRepository courseRepository;
    private final LlmProvider llmProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public SendMessageOutput execute(SendMessageInput input) {
        ChatSession session = chatSessionRepository.findById(input.getSessionId())
                .filter(s -> s.getUserId().equals(input.getUserId()))
                .orElseThrow(() -> new IllegalArgumentException("Session not found or forbidden"));

        // 1. Save user message
        ChatMessage userMessage = ChatMessage.create(session.getId(), ChatRole.USER, input.getContent(), null);
        chatMessageRepository.save(userMessage);

        // 2. Fetch history
        List<ChatMessage> fullHistory = chatMessageRepository.findBySessionId(session.getId());
        
        // 3. Context Compaction
        if (fullHistory.size() >= 10 && fullHistory.size() % 10 == 0) {
            List<LlmProvider.Message> historyForSummary = fullHistory.stream()
                    .map(m -> LlmProvider.Message.builder().role(m.getRole()).content(m.getContent()).build())
                    .collect(Collectors.toList());
            String newSummary = llmProvider.summarize(historyForSummary);
            session.setSummary(newSummary);
            chatSessionRepository.save(session);
        }

        // 4. Prepare context (summary + latest messages)
        List<LlmProvider.Message> llmHistory = new ArrayList<>();
        if (session.getSummary() != null) {
            llmHistory.add(LlmProvider.Message.builder()
                    .role(ChatRole.SYSTEM)
                    .content("Summary of previous conversation: " + session.getSummary())
                    .build());
        }
        
        // Take last 10 messages for context
        int skip = Math.max(0, fullHistory.size() - 10);
        fullHistory.stream().skip(skip).forEach(m -> {
            llmHistory.add(LlmProvider.Message.builder()
                    .role(m.getRole())
                    .content(m.getContent())
                    .build());
        });

        // 5. Fetch course context (Basic RAG)
        List<LlmProvider.CourseContext> courseContext = courseRepository.searchPublishedCourses(null, 0, 10)
                .items().stream()
                .map(c -> LlmProvider.CourseContext.builder()
                        .id(c.getId())
                        .title(c.getTitle())
                        .description(c.getDescription())
                        .build())
                .collect(Collectors.toList());

        // 6. Call LLM
        LlmProvider.ChatResponse response = llmProvider.chat(llmHistory, courseContext, input.getContent());

        // 7. Save bot message
        String recommendedIdsJson = null;
        try {
            if (response.getRecommendedCourseIds() != null && !response.getRecommendedCourseIds().isEmpty()) {
                recommendedIdsJson = objectMapper.writeValueAsString(response.getRecommendedCourseIds());
            }
        } catch (JsonProcessingException e) {
            // Ignore serialization error
        }

        ChatMessage botMessage = ChatMessage.create(session.getId(), ChatRole.ASSISTANT, response.getReplyMessage(), recommendedIdsJson);
        chatMessageRepository.save(botMessage);

        return SendMessageOutput.builder()
                .replyMessage(response.getReplyMessage())
                .recommendedCourseIds(response.getRecommendedCourseIds())
                .build();
    }
}
