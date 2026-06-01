package com.example.learning_system_spring.adapter.controller;

import com.example.learning_system_spring.adapter.dto.request.Permission.UpdateRolePermissionsRequest;
import com.example.learning_system_spring.adapter.dto.response.ApiResponse;
import com.example.learning_system_spring.adapter.dto.response.PermissionMatrixResponse;
import com.example.learning_system_spring.adapter.dto.response.PermissionResponse;
import com.example.learning_system_spring.adapter.dto.response.RolePermissionsResponse;
import com.example.learning_system_spring.application.dto.Permission.PermissionMatrixOutput;
import com.example.learning_system_spring.application.dto.Permission.RolePermissionsOutput;
import com.example.learning_system_spring.application.usecase.Permission.GetAllPermissionsUseCase;
import com.example.learning_system_spring.application.usecase.Permission.GetPermissionMatrixUseCase;
import com.example.learning_system_spring.application.usecase.Permission.GetRolePermissionsUseCase;
import com.example.learning_system_spring.application.usecase.Permission.UpdateRolePermissionsUseCase;
import com.example.learning_system_spring.infrastructure.config.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Quản trị phân quyền động (Dynamic RBAC). Chỉ user có permission {@code MANAGE_ROLE}.
 *
 * Đây là nhóm endpoint đầu tiên enforce bằng {@code hasAuthority('PERMISSION')} thay vì
 * {@code hasRole(...)} — theo chiến lược migrate dần (chốt 2.2).
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminPermissionController {

    private final GetAllPermissionsUseCase getAllPermissionsUseCase;
    private final GetPermissionMatrixUseCase getPermissionMatrixUseCase;
    private final GetRolePermissionsUseCase getRolePermissionsUseCase;
    private final UpdateRolePermissionsUseCase updateRolePermissionsUseCase;
    private final JwtService jwtService;

    private Long getUserId(HttpServletRequest request) {
        Claims claims = jwtService.parseToken(request.getHeader("Authorization").substring(7));
        return claims.get("userId", Long.class);
    }

    /** Danh sách tất cả permission khả dụng trong hệ thống. */
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('MANAGE_ROLE')")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> listPermissions() {
        List<PermissionResponse> data = getAllPermissionsUseCase.execute().stream()
                .map(PermissionResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /** Toàn bộ ma trận role → permission (FE render bảng checkbox). */
    @GetMapping("/roles/permissions")
    @PreAuthorize("hasAuthority('MANAGE_ROLE')")
    public ResponseEntity<ApiResponse<PermissionMatrixResponse>> getMatrix() {
        PermissionMatrixOutput output = getPermissionMatrixUseCase.execute();
        return ResponseEntity.ok(ApiResponse.success(PermissionMatrixResponse.from(output)));
    }

    /** Permission của một role cụ thể. */
    @GetMapping("/roles/{roleName}/permissions")
    @PreAuthorize("hasAuthority('MANAGE_ROLE')")
    public ResponseEntity<ApiResponse<RolePermissionsResponse>> getRolePermissions(
            @PathVariable String roleName) {
        RolePermissionsOutput output = getRolePermissionsUseCase.execute(roleName);
        return ResponseEntity.ok(ApiResponse.success(RolePermissionsResponse.from(output)));
    }

    /** Thay thế toàn bộ permission của một role (replace-all). */
    @PutMapping("/roles/{roleName}/permissions")
    @PreAuthorize("hasAuthority('MANAGE_ROLE')")
    public ResponseEntity<ApiResponse<RolePermissionsResponse>> updateRolePermissions(
            @PathVariable String roleName,
            @Valid @RequestBody UpdateRolePermissionsRequest req,
            HttpServletRequest request) {

        RolePermissionsOutput output = updateRolePermissionsUseCase.execute(
                req.toInput(roleName, getUserId(request)));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật phân quyền thành công",
                RolePermissionsResponse.from(output)));
    }
}
