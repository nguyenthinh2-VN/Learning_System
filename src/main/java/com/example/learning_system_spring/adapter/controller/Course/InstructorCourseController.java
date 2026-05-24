package com.example.learning_system_spring.adapter.controller.Course;

import com.example.learning_system_spring.adapter.dto.response.CourseDetailResponse;
import com.example.learning_system_spring.adapter.dto.response.CourseListResponse;
import com.example.learning_system_spring.application.dto.Course.CourseOutput;
import com.example.learning_system_spring.application.dto.Course.GetCourseDetailInput;
import com.example.learning_system_spring.application.dto.Course.GetCourseListInput;
import com.example.learning_system_spring.application.usecase.Course.GetCourseDetailUseCase;
import com.example.learning_system_spring.application.usecase.Course.GetCourseListUseCase;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.infrastructure.config.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Endpoint cho INSTRUCTOR xem danh sách / chi tiết course của chính mình
 * (kể cả course chưa publish).
 */
@RestController
@RequestMapping("/api/v1/instructor/courses")
@RequiredArgsConstructor
public class InstructorCourseController {

    private final GetCourseListUseCase getCourseListUseCase;
    private final GetCourseDetailUseCase getCourseDetailUseCase;
    private final JwtService jwtService;

    private Claims getClaims(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtService.parseToken(token);
    }

    @GetMapping
    public ResponseEntity<?> myCourses(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);
        Role requesterRole = Role.reconstitute(null, claims.get("role", String.class), null);

        GetCourseListInput input = GetCourseListInput.instructorScope(
                keyword, page, size, requesterId, requesterRole);

        var output = getCourseListUseCase.execute(input);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Success",
                "data", CourseListResponse.from(output),
                "timestamp", LocalDateTime.now()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> myCourseDetail(@PathVariable Long id, HttpServletRequest request) {
        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);
        Role requesterRole = Role.reconstitute(null, claims.get("role", String.class), null);

        GetCourseDetailInput input = new GetCourseDetailInput(id, requesterId, requesterRole);
        CourseOutput output = getCourseDetailUseCase.execute(input);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Success",
                "data", CourseDetailResponse.from(output),
                "timestamp", LocalDateTime.now()));
    }
}
