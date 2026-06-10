package com.example.learning_system_spring.adapter.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SendMessageRequest {
    @NotBlank(message = "Message content cannot be blank")
    @Size(max = 2000, message = "Message is too long")
    private String content;
}
