package com.example.learning_system_spring.application.port;

import com.example.learning_system_spring.domain.model.ChatRole;
import lombok.Builder;
import lombok.Data;

import java.util.List;

public interface LlmProvider {

    @Data
    @Builder
    class Message {
        private ChatRole role;
        private String content;
    }

    @Data
    @Builder
    class CourseContext {
        private Long id;
        private String title;
        private String description;
    }

    @Data
    @Builder
    class ChatResponse {
        private String replyMessage;
        private List<Long> recommendedCourseIds;
    }

    ChatResponse chat(List<Message> history, List<CourseContext> context, String userMessage);

    String summarize(List<Message> history);
}
