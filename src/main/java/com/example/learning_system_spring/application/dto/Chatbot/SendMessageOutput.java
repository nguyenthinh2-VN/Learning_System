package com.example.learning_system_spring.application.dto.Chatbot;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SendMessageOutput {
    private String replyMessage;
    private List<Long> recommendedCourseIds;
}
