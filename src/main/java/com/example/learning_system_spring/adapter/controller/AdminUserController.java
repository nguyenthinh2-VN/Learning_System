package com.example.learning_system_spring.adapter.controller;

import com.example.learning_system_spring.adapter.dto.request.CreateUserRequest;
import com.example.learning_system_spring.adapter.dto.request.User.AdminUpdateUserRequest;
import com.example.learning_system_spring.adapter.dto.request.User.AdminUpdateUserStatusRequest;
import com.example.learning_system_spring.adapter.dto.request.Wallet.AdminTopUpByIdentifierRequest;
import com.example.learning_system_spring.adapter.dto.request.Wallet.AdminTopUpRequest;
import com.example.learning_system_spring.adapter.dto.response.AdminUserDetailResponse;
import com.example.learning_system_spring.adapter.dto.response.ApiResponse;
import com.example.learning_system_spring.adapter.dto.response.RegisterResponse;
import com.example.learning_system_spring.adapter.dto.response.UserListResponse;
import com.example.learning_system_spring.application.dto.Auth.RegisterOutput;
import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.application.dto.User.AdminUserDetailOutput;
import com.example.learning_system_spring.application.dto.User.UserListOutput;
import com.example.learning_system_spring.application.dto.Wallet.AdminTopUpOutput;
import com.example.learning_system_spring.application.usecase.User.AdminCreateUserUseCase;
import com.example.learning_system_spring.application.usecase.User.AdminSetUserStatusUseCase;
import com.example.learning_system_spring.application.usecase.User.AdminUpdateUserUseCase;
import com.example.learning_system_spring.application.usecase.User.GetUsersUseCase;
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
    private final GetUsersUseCase getUsersUseCase;
    private final AdminUpdateUserUseCase adminUpdateUserUseCase;
    private final AdminSetUserStatusUseCase adminSetUserStatusUseCase;
    private final WalletNotificationService walletNotificationService;
    private final JwtService jwtService;

    private Claims getClaims(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtService.parseToken(token);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_USER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PageResult<UserListResponse>>> listUsers(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<UserListOutput> result = getUsersUseCase.execute(keyword, page, size);
        PageResult<UserListResponse> response = result.map(UserListResponse::from);
        return ResponseEntity.ok(ApiResponse.success("Success", response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_USER', 'SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<RegisterResponse>> createUser(@Valid @RequestBody CreateUserRequest req) {
        RegisterOutput output = adminCreateUserUseCase.execute(req.toInput());
        return ResponseEntity.status(201).body(ApiResponse.created(RegisterResponse.from(output)));
    }

    /**
     * Admin cập nhật thông tin user: name, roleName, isInternal (field null = giữ nguyên).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_USER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequest req,
            HttpServletRequest request) {

        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);
        String requesterRole = claims.get("role", String.class);

        AdminUserDetailOutput output = adminUpdateUserUseCase.execute(
                req.toInput(id, requesterId, requesterRole));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật người dùng thành công",
                AdminUserDetailResponse.from(output)));
    }

    /**
     * Admin khóa / mở khóa tài khoản user.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN_USER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> setUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserStatusRequest req,
            HttpServletRequest request) {

        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);
        String requesterRole = claims.get("role", String.class);

        AdminUserDetailOutput output = adminSetUserStatusUseCase.execute(
                req.toInput(id, requesterId, requesterRole));
        String msg = output.enabled() ? "Đã mở khóa tài khoản" : "Đã khóa tài khoản";
        return ResponseEntity.ok(ApiResponse.success(msg, AdminUserDetailResponse.from(output)));
    }

    /**
     * Admin cộng tiền thủ công cho user bất kỳ (theo userId — giữ cho tương thích ngược).
     * Sau khi cộng tiền, push WebSocket event tới FE của user đó.
     */
    @PostMapping("/{userId}/top-up")
    @PreAuthorize("hasAuthority('MANAGE_WALLET')")
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

    /**
     * Admin cộng tiền thủ công theo username HOẶC email (ô nhập 1 dòng — dễ nhớ hơn userId).
     * Phân giải identifier ra user rồi cộng tiền. Push WebSocket sau khi cộng.
     */
    @PostMapping("/top-up")
    @PreAuthorize("hasAuthority('MANAGE_WALLET')")
    public ResponseEntity<ApiResponse<AdminTopUpOutput>> adminTopUpByIdentifier(
            @Valid @RequestBody AdminTopUpByIdentifierRequest request) {

        AdminTopUpOutput output = adminTopUpUseCase.execute(
                request.identifier(), request.amount(), request.note());

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
