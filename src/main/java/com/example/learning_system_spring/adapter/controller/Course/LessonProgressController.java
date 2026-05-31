package com.example.learning_system_spring.adapter.controller.Course;

import com.example.learning_system_spring.adapter.dto.response.ApiResponse;
import com.example.learning_system_spring.adapter.dto.response.CourseProgressResponse;
import com.example.learning_system_spring.application.dto.Progress.CourseProgressOutput;
import com.example.learning_system_spring.application.usecase.Progress.GetCourseProgressUseCase;
import com.example.learning_system_spring.application.usecase.Progress.MarkLessonCompleteUseCase;
import com.example.learning_system_spring.application.usecase.Progress.UnmarkLessonCompleteUseCase;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.infrastructure.config.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Theo dõi tiến độ học (Lesson Progress).
 *
 * - POST   /api/v1/courses/{courseId}/lessons/{lessonId}/complete  → đánh dấu đã học
 * - DELETE /api/v1/courses/{courseId}/lessons/{lessonId}/complete  → bỏ đánh dấu
 * - GET    /api/v1/courses/{courseId}/progress                     → tiến độ tổng quan
 *
 * Quyền chi tiết do use case raise (member phải enrolled, instructor phải owner, ...).
 */
@RestController
@RequestMapping("/api/v1/courses/{courseId}")
@RequiredArgsConstructor
public class LessonProgressController {

    private final MarkLessonCompleteUseCase markLessonCompleteUseCase;
    private final UnmarkLessonCompleteUseCase unmarkLessonCompleteUseCase;
    private final GetCourseProgressUseCase getCourseProgressUseCase;
    private final JwtService jwtService;

    private Claims getClaims(HttpServletRequest request) {
        return jwtService.parseToken(request.getHeader("Authorization").substring(7));
    }

    @PostMapping("/lessons/{lessonId}/complete")
    @PreAuthorize("hasAnyRole('MEMBER', 'INSTRUCTOR', 'STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markComplete(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            HttpServletRequest request) {

        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);
        Role role = Role.create(claims.get("role", String.class), "");

        markLessonCompleteUseCase.execute(requesterId, role, courseId, lessonId);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu hoàn thành",
                Map.of("lessonId", lessonId, "completed", true)));
    }

    @DeleteMapping("/lessons/{lessonId}/complete")
    @PreAuthorize("hasAnyRole('MEMBER', 'INSTRUCTOR', 'STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unmarkComplete(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            HttpServletRequest request) {

        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);
        Role role = Role.create(claims.get("role", String.class), "");

        unmarkLessonCompleteUseCase.execute(requesterId, role, courseId, lessonId);
        return ResponseEntity.ok(ApiResponse.success("Đã bỏ đánh dấu",
                Map.of("lessonId", lessonId, "completed", false)));
    }

    @GetMapping("/progress")
    @PreAuthorize("hasAnyRole('MEMBER', 'INSTRUCTOR', 'STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CourseProgressResponse>> getProgress(
            @PathVariable Long courseId,
            HttpServletRequest request) {

        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);
        Role role = Role.create(claims.get("role", String.class), "");

        CourseProgressOutput output = getCourseProgressUseCase.execute(requesterId, role, courseId);
        return ResponseEntity.ok(ApiResponse.success(CourseProgressResponse.from(output)));
    }
}
