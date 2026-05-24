package com.example.learning_system_spring.adapter.controller;

import com.example.learning_system_spring.adapter.dto.request.Course.UpdateCoursePriceRequest;
import com.example.learning_system_spring.adapter.dto.response.CourseDetailResponse;
import com.example.learning_system_spring.adapter.dto.response.CourseListResponse;
import com.example.learning_system_spring.application.dto.Course.CourseOutput;
import com.example.learning_system_spring.application.dto.Course.GetCourseListInput;
import com.example.learning_system_spring.application.dto.Course.PublishCourseInput;
import com.example.learning_system_spring.application.dto.Course.UnpublishCourseInput;
import com.example.learning_system_spring.application.dto.Course.UpdateCoursePriceInput;
import com.example.learning_system_spring.application.usecase.Course.GetCourseListUseCase;
import com.example.learning_system_spring.application.usecase.Course.PublishCourseUseCase;
import com.example.learning_system_spring.application.usecase.Course.UnpublishCourseUseCase;
import com.example.learning_system_spring.application.usecase.Course.UpdateCoursePriceUseCase;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.infrastructure.config.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Endpoint quản trị course cho STAFF / SUPER_ADMIN: duyệt course, ẩn / hiện,
 * cập nhật giá (kể cả khi đã priceLocked).
 */
@RestController
@RequestMapping("/api/v1/admin/courses")
@RequiredArgsConstructor
public class AdminCourseController {

    private final GetCourseListUseCase getCourseListUseCase;
    private final PublishCourseUseCase publishCourseUseCase;
    private final UnpublishCourseUseCase unpublishCourseUseCase;
    private final UpdateCoursePriceUseCase updateCoursePriceUseCase;
    private final JwtService jwtService;

    private Claims getClaims(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtService.parseToken(token);
    }

    @GetMapping("/pending")
    public ResponseEntity<?> pendingCourses(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);
        Role requesterRole = Role.reconstitute(null, claims.get("role", String.class), null);

        GetCourseListInput input = GetCourseListInput.pendingScope(keyword, page, size, requesterId, requesterRole);
        var output = getCourseListUseCase.execute(input);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Success",
                "data", CourseListResponse.from(output),
                "timestamp", LocalDateTime.now()));
    }

    @GetMapping
    public ResponseEntity<?> allCourses(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);
        Role requesterRole = Role.reconstitute(null, claims.get("role", String.class), null);

        GetCourseListInput input = GetCourseListInput.allScope(keyword, page, size, requesterId, requesterRole);
        var output = getCourseListUseCase.execute(input);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Success",
                "data", CourseListResponse.from(output),
                "timestamp", LocalDateTime.now()));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publish(@PathVariable Long id, HttpServletRequest request) {
        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);
        Role requesterRole = Role.reconstitute(null, claims.get("role", String.class), null);

        PublishCourseInput input = new PublishCourseInput(id, requesterId, requesterRole);
        CourseOutput output = publishCourseUseCase.execute(input);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Khóa học đã được duyệt và public",
                "data", CourseDetailResponse.from(output),
                "timestamp", LocalDateTime.now()));
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<?> unpublish(@PathVariable Long id, HttpServletRequest request) {
        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);
        Role requesterRole = Role.reconstitute(null, claims.get("role", String.class), null);

        UnpublishCourseInput input = new UnpublishCourseInput(id, requesterId, requesterRole);
        CourseOutput output = unpublishCourseUseCase.execute(input);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Khóa học đã được ẩn khỏi public",
                "data", CourseDetailResponse.from(output),
                "timestamp", LocalDateTime.now()));
    }

    @PutMapping("/{id}/price")
    public ResponseEntity<?> updatePrice(@PathVariable Long id,
                                         @Valid @RequestBody UpdateCoursePriceRequest req,
                                         HttpServletRequest request) {
        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);
        Role requesterRole = Role.reconstitute(null, claims.get("role", String.class), null);

        UpdateCoursePriceInput input = new UpdateCoursePriceInput(id, requesterId, requesterRole, req.getPrice());
        CourseOutput output = updateCoursePriceUseCase.execute(input);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Giá khóa học đã được cập nhật",
                "data", CourseDetailResponse.from(output),
                "timestamp", LocalDateTime.now()));
    }
}
