package com.example.learning_system_spring.adapter.controller;

import com.example.learning_system_spring.adapter.dto.request.SendMessageRequest;
import com.example.learning_system_spring.adapter.dto.response.ApiResponse;
import com.example.learning_system_spring.application.dto.Chatbot.ChatMessageOutput;
import com.example.learning_system_spring.application.dto.Chatbot.CreateSessionOutput;
import com.example.learning_system_spring.application.dto.Chatbot.SendMessageInput;
import com.example.learning_system_spring.application.dto.Chatbot.SendMessageOutput;
import com.example.learning_system_spring.application.usecase.Chatbot.CreateSessionUseCase;
import com.example.learning_system_spring.application.usecase.Chatbot.GetMessagesUseCase;
import com.example.learning_system_spring.application.usecase.Chatbot.SendMessageUseCase;
import com.example.learning_system_spring.infrastructure.config.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
@Validated
public class ChatbotController {

    private final CreateSessionUseCase createSessionUseCase;
    private final GetMessagesUseCase getMessagesUseCase;
    private final SendMessageUseCase sendMessageUseCase;
    private final JwtService jwtService;

    private Claims getClaims(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtService.parseToken(token);
    }

    private Long getUserId(HttpServletRequest request) {
        return getClaims(request).get("userId", Long.class);
    }

    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<CreateSessionOutput>> createSession(HttpServletRequest request) {
        Long userId = getUserId(request);
        CreateSessionOutput output = createSessionUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.success(output));
    }

    @GetMapping("/sessions/{id}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageOutput>>> getMessages(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        List<ChatMessageOutput> messages = getMessagesUseCase.execute(userId, id);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PostMapping("/sessions/{id}/messages")
    public ResponseEntity<ApiResponse<SendMessageOutput>> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest requestBody,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        SendMessageInput input = SendMessageInput.builder()
                .userId(userId)
                .sessionId(id)
                .content(requestBody.getContent())
                .build();
        SendMessageOutput output = sendMessageUseCase.execute(input);
        return ResponseEntity.ok(ApiResponse.success(output));
    }
}
