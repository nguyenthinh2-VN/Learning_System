package com.example.learning_system_spring.adapter.controller;

import com.example.learning_system_spring.adapter.dto.request.CreateUserRequest;
import com.example.learning_system_spring.adapter.dto.request.Wallet.AdminTopUpRequest;
import com.example.learning_system_spring.adapter.dto.response.ApiResponse;
import com.example.learning_system_spring.adapter.dto.response.RegisterResponse;
import com.example.learning_system_spring.application.dto.Auth.RegisterOutput;
import com.example.learning_system_spring.application.dto.Wallet.AdminTopUpOutput;
import com.example.learning_system_spring.application.usecase.User.AdminCreateUserUseCase;
import com.example.learning_system_spring.application.usecase.Wallet.AdminTopUpUseCase;
import com.example.learning_system_spring.infrastructure.config.JwtService;
import com.example.learning_system_spring.infrastructure.service.WalletNotificationService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminCreateUserUseCase adminCreateUserUseCase;
    private final AdminTopUpUseCase adminTopUpUseCase;
    private final WalletNotificationService walletNotificationService;
    private final JwtService jwtService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_USER', 'SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<RegisterResponse>> createUser(@Valid @RequestBody CreateUserRequest req) {
        RegisterOutput output = adminCreateUserUseCase.execute(req.toInput());
        return ResponseEntity.status(201).body(ApiResponse.created(RegisterResponse.from(output)));
    }

    /**
     * Admin cộng tiền thủ công cho user bất kỳ.
     * Sau khi cộng tiền, push WebSocket event tới FE của user đó.
     */
    @PostMapping("/{userId}/top-up")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminTopUpOutput>> adminTopUp(
            @PathVariable Long userId,
            @Valid @RequestBody AdminTopUpRequest request) {

        AdminTopUpOutput output = adminTopUpUseCase.execute(userId, request.amount(), request.note());

        // Push WebSocket tới FE của user được cộng tiền
        walletNotificationService.pushWalletUpdated(
                output.username(),
                output.userId(),
                output.newBalance(),
                output.addedAmount(),
                "ADMIN",
                output.referenceCode(),
                output.note()
        );

        return ResponseEntity.ok(ApiResponse.success("Cộng tiền thành công", output));
    }
}
