package com.example.learning_system_spring.adapter.controller.Course;

import com.example.learning_system_spring.adapter.dto.request.SubmitSectionTestRequest;
import com.example.learning_system_spring.adapter.dto.response.ApiResponse;
import com.example.learning_system_spring.application.usecase.Course.SubmitSectionTestUseCase;
import com.example.learning_system_spring.application.usecase.Course.GetSectionTestUseCase;
import com.example.learning_system_spring.infrastructure.config.JwtService;

import java.util.List;
import java.util.Map;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sections")
@RequiredArgsConstructor
public class SectionTestController {

    private final SubmitSectionTestUseCase submitSectionTestUseCase;
    private final GetSectionTestUseCase getSectionTestUseCase;
    private final JwtService jwtService;

    @GetMapping("/{sectionId}/test")
    @PreAuthorize("hasAnyRole('MEMBER', 'INSTRUCTOR', 'STAFF', 'ADMIN_USER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTest(@PathVariable Long sectionId) {
        List<Map<String, Object>> test = getSectionTestUseCase.execute(sectionId);
        return ResponseEntity.ok(ApiResponse.success(test));
    }

    @PostMapping("/{sectionId}/submit-test")
    @PreAuthorize("hasAnyRole('MEMBER', 'INSTRUCTOR', 'STAFF', 'ADMIN_USER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SubmitSectionTestUseCase.SubmitTestResult>> submitTest(
            @PathVariable Long sectionId,
            @RequestBody SubmitSectionTestRequest request,
            HttpServletRequest httpServletRequest) {
        String token = httpServletRequest.getHeader("Authorization").substring(7);
        Claims claims = jwtService.parseToken(token);
        Long userId = claims.get("userId", Long.class);

        SubmitSectionTestUseCase.SubmitTestResult result = submitSectionTestUseCase.execute(sectionId, request, userId);

        return ResponseEntity.ok(ApiResponse.success("Test submitted", result));
    }
}
