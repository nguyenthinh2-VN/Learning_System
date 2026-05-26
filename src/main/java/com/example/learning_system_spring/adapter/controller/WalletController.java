package com.example.learning_system_spring.adapter.controller;

import com.example.learning_system_spring.adapter.dto.request.Wallet.InitTopUpRequest;
import com.example.learning_system_spring.adapter.dto.response.ApiResponse;
import com.example.learning_system_spring.application.dto.Wallet.InitTopUpOutput;
import com.example.learning_system_spring.application.usecase.Wallet.InitTopUpUseCase;
import com.example.learning_system_spring.infrastructure.config.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final InitTopUpUseCase initTopUpUseCase;
    private final JwtService jwtService;

    /**
     * User khởi tạo yêu cầu nạp tiền.
     * Trả về thông tin để FE hiển thị (QR URL hoặc hướng dẫn mock).
     */
    @PostMapping("/top-up/init")
    public ResponseEntity<ApiResponse<InitTopUpOutput>> initTopUp(
            @Valid @RequestBody InitTopUpRequest request,
            HttpServletRequest httpRequest) {

        Claims claims = parseClaims(httpRequest);
        Long userId = claims.get("userId", Long.class);

        InitTopUpOutput output = initTopUpUseCase.execute(userId, request.amount());

        return ResponseEntity.ok(
                ApiResponse.success("Tạo yêu cầu nạp tiền thành công", output));
    }

    private Claims parseClaims(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtService.parseToken(token);
    }
}
