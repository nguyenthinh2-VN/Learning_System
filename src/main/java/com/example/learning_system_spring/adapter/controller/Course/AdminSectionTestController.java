package com.example.learning_system_spring.adapter.controller.Course;

import com.example.learning_system_spring.adapter.dto.response.ApiResponse;
import com.example.learning_system_spring.application.usecase.Course.AdminUpdateSectionTestUseCase;
import com.example.learning_system_spring.application.usecase.Course.AdminGetSectionTestUseCase;
import com.example.learning_system_spring.infrastructure.config.JwtService;
import com.example.learning_system_spring.domain.model.Role;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/sections")
@RequiredArgsConstructor
public class AdminSectionTestController {

    private final AdminUpdateSectionTestUseCase adminUpdateSectionTestUseCase;
    private final AdminGetSectionTestUseCase adminGetSectionTestUseCase;
    private final JwtService jwtService;

    @GetMapping("/{sectionId}/test")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> getSectionTest(
            @PathVariable Long sectionId,
            HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Claims claims = jwtService.parseToken(token);
        Long requesterId = claims.get("userId", Long.class);
        Role requesterRole = Role.reconstitute(null, claims.get("role", String.class), null);

        String testContent = adminGetSectionTestUseCase.execute(sectionId, requesterId, requesterRole);

        return ResponseEntity.ok(ApiResponse.success(testContent));
    }

    @PutMapping("/{sectionId}/test")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateSectionTest(
            @PathVariable Long sectionId,
            @RequestBody String testContent,
            HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Claims claims = jwtService.parseToken(token);
        Long requesterId = claims.get("userId", Long.class);
        Role requesterRole = Role.reconstitute(null, claims.get("role", String.class), null);

        adminUpdateSectionTestUseCase.execute(sectionId, testContent, requesterId, requesterRole);

        return ResponseEntity.ok(ApiResponse.success("Test updated successfully", null));
    }
}
