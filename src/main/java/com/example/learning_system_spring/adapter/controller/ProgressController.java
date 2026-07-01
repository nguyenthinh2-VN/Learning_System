package com.example.learning_system_spring.adapter.controller;

import com.example.learning_system_spring.application.dto.Progress.ProgressDataOutput;
import com.example.learning_system_spring.application.usecase.Progress.GetProgressUseCase;
import com.example.learning_system_spring.infrastructure.config.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final GetProgressUseCase getProgressUseCase;
    private final JwtService jwtService;

    private Long getUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Claims claims = jwtService.parseToken(token);
        return claims.get("userId", Long.class);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProgressDataOutput> getProgress(HttpServletRequest request) {
        Long userId = getUserId(request);
        ProgressDataOutput output = getProgressUseCase.execute(userId);
        return ResponseEntity.ok(output);
    }
}
