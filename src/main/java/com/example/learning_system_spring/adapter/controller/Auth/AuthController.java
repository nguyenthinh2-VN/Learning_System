package com.example.learning_system_spring.adapter.controller.Auth;

import com.example.learning_system_spring.adapter.dto.request.LoginRequest;
import com.example.learning_system_spring.adapter.dto.request.RegisterRequest;
import com.example.learning_system_spring.adapter.dto.response.ApiResponse;
import com.example.learning_system_spring.adapter.dto.response.LoginResponse;
import com.example.learning_system_spring.adapter.dto.response.RegisterResponse;
import com.example.learning_system_spring.application.dto.Auth.LoginOutput;
import com.example.learning_system_spring.application.dto.Auth.RegisterOutput;
import com.example.learning_system_spring.application.usecase.Auth.LoginUseCase;
import com.example.learning_system_spring.application.usecase.Auth.RegisterUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
    @RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest req) {
        RegisterOutput output = registerUseCase.execute(req.toInput());
        return ResponseEntity.status(201).body(ApiResponse.created(RegisterResponse.from(output)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest req) {
        LoginOutput output = loginUseCase.execute(req.toInput());
        return ResponseEntity.ok(ApiResponse.success(LoginResponse.from(output)));
    }
}
