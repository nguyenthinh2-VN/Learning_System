package com.example.learning_system_spring.application.dto.Chatbot;

import com.example.learning_system_spring.domain.model.ChatSession;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateSessionOutput {
    private Long id;
    private String title;

    public static CreateSessionOutput from(ChatSession session) {
        return CreateSessionOutput.builder()
                .id(session.getId())
                .title(session.getTitle())
                .build();
    }
}
