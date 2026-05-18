package com.example.learning_system_spring.adapter.controller;

import com.example.learning_system_spring.application.usecase.User.TopUpBalanceUseCase;
import com.example.learning_system_spring.infrastructure.config.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final TopUpBalanceUseCase topUpBalanceUseCase;
    private final JwtService jwtService;

    private Claims getClaims(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtService.parseToken(token);
    }

    @PostMapping("/me/top-up")
    public ResponseEntity<?> topUp(
            @RequestBody Map<String, BigDecimal> requestBody,
            HttpServletRequest request) {

        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);

        BigDecimal amount = requestBody.get("amount");
        BigDecimal newBalance = topUpBalanceUseCase.execute(requesterId, amount);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Nạp tiền thành công",
                "data", Map.of("newBalance", newBalance),
                "timestamp", LocalDateTime.now()));
    }
}
