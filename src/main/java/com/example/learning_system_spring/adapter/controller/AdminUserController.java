package com.example.learning_system_spring.adapter.controller;

import com.example.learning_system_spring.adapter.dto.request.CreateUserRequest;
import com.example.learning_system_spring.adapter.dto.response.ApiResponse;
import com.example.learning_system_spring.adapter.dto.response.RegisterResponse;
import com.example.learning_system_spring.application.dto.Auth.RegisterOutput;
import com.example.learning_system_spring.application.usecase.User.AdminCreateUserUseCase;
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

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_USER', 'SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<RegisterResponse>> createUser(@Valid @RequestBody CreateUserRequest req) {
        RegisterOutput output = adminCreateUserUseCase.execute(req.toInput());
        return ResponseEntity.status(201).body(ApiResponse.created(RegisterResponse.from(output)));
    }
}
