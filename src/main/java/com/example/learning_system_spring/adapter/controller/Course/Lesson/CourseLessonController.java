package com.example.learning_system_spring.adapter.controller.Course.Lesson;

import com.example.learning_system_spring.adapter.dto.request.Lesson.CreateLessonRequest;
import com.example.learning_system_spring.adapter.dto.request.Lesson.UpdateLessonRequest;
import com.example.learning_system_spring.adapter.dto.response.ApiResponse;
import com.example.learning_system_spring.adapter.dto.response.GetLessonsResponse;
import com.example.learning_system_spring.adapter.dto.response.LessonResponse;
import com.example.learning_system_spring.application.dto.Lesson.DeleteLessonInput;
import com.example.learning_system_spring.application.dto.Lesson.GetLessonsInput;
import com.example.learning_system_spring.application.usecase.Lesson.CreateLessonUseCase;
import com.example.learning_system_spring.application.usecase.Lesson.DeleteLessonUseCase;
import com.example.learning_system_spring.application.usecase.Lesson.GetLessonsUseCase;
import com.example.learning_system_spring.application.usecase.Lesson.UpdateLessonUseCase;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.infrastructure.config.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/sections/{sectionId}/lessons")
@RequiredArgsConstructor
public class CourseLessonController {
    private final GetLessonsUseCase getLessonsUseCase;
    private final CreateLessonUseCase createLessonUseCase;
    private final UpdateLessonUseCase updateLessonUseCase;
    private final DeleteLessonUseCase deleteLessonUseCase;
    private final JwtService jwtService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MEMBER', 'INSTRUCTOR', 'STAFF', 'ADMIN_USER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<GetLessonsResponse>> getLessons(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @RequestHeader("Authorization") String authHeader) {

        var claims = jwtService.parseToken(authHeader.substring(7));
        Long requesterId = claims.get("userId", Long.class);
        String requesterRoleName = claims.get("role", String.class);
        Role requesterRole = Role.create(requesterRoleName, "");

        var input = new GetLessonsInput(courseId, sectionId, requesterId, requesterRole);
        var output = getLessonsUseCase.execute(input);
        var response = GetLessonsResponse.from(output);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<LessonResponse>> createLesson(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @Valid @RequestBody CreateLessonRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        // Parse JWT để lấy thông tin người dùng
        var claims = jwtService.parseToken(authHeader.substring(7));
        Long requesterId = claims.get("userId", Long.class);
        String requesterRoleName = claims.get("role", String.class);
        Role requesterRole = Role.create(requesterRoleName, "");
        
        var input = request.toInput(courseId, sectionId, requesterId, requesterRole);
        var output = createLessonUseCase.execute(input);
        var response = LessonResponse.from(output);
        
        return ResponseEntity.status(201).body(ApiResponse.created(response));
    }

    @PutMapping("/{lessonId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<LessonResponse>> updateLesson(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @PathVariable Long lessonId,
            @Valid @RequestBody UpdateLessonRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        // Parse JWT để lấy thông tin người dùng
        var claims = jwtService.parseToken(authHeader.substring(7));
        Long requesterId = claims.get("userId", Long.class);
        String requesterRoleName = claims.get("role", String.class);
        Role requesterRole = Role.create(requesterRoleName, "");
        
        var input = request.toInput(lessonId, courseId, sectionId, requesterId, requesterRole);
        var output = updateLessonUseCase.execute(input);
        var response = LessonResponse.from(output);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{lessonId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteLesson(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @PathVariable Long lessonId,
            @RequestHeader("Authorization") String authHeader) {
        
        // Parse JWT để lấy thông tin người dùng
        var claims = jwtService.parseToken(authHeader.substring(7));
        Long requesterId = claims.get("userId", Long.class);
        String requesterRoleName = claims.get("role", String.class);
        Role requesterRole = Role.create(requesterRoleName, "");
        
        var input = new DeleteLessonInput(lessonId, courseId, sectionId, requesterId, requesterRole);
        deleteLessonUseCase.execute(input);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}