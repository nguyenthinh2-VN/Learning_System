package com.example.learning_system_spring.adapter.controller.Course;

import com.example.learning_system_spring.adapter.dto.request.Section.CreateSectionRequest;
import com.example.learning_system_spring.adapter.dto.request.Section.UpdateSectionRequest;
import com.example.learning_system_spring.adapter.dto.response.ApiResponse;
import com.example.learning_system_spring.adapter.dto.response.SectionResponse;
import com.example.learning_system_spring.application.dto.Section.CreateSectionInput;
import com.example.learning_system_spring.application.dto.Section.DeleteSectionInput;
import com.example.learning_system_spring.application.dto.Section.SectionOutput;
import com.example.learning_system_spring.application.dto.Section.UpdateSectionInput;
import com.example.learning_system_spring.application.usecase.Section.CreateSectionUseCase;
import com.example.learning_system_spring.application.usecase.Section.DeleteSectionUseCase;
import com.example.learning_system_spring.application.usecase.Section.GetSectionsUseCase;
import com.example.learning_system_spring.application.usecase.Section.UpdateSectionUseCase;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.infrastructure.config.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/sections")
@RequiredArgsConstructor
public class CourseSectionController {

    private final GetSectionsUseCase getSectionsUseCase;
    private final CreateSectionUseCase createSectionUseCase;
    private final UpdateSectionUseCase updateSectionUseCase;
    private final DeleteSectionUseCase deleteSectionUseCase;
    private final JwtService jwtService;

    private Claims getClaims(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtService.parseToken(token);
    }

    /**
     * GET /api/v1/courses/{courseId}/sections
     * Lấy danh sách sections (kèm lessons) của một course.
     * Tất cả role đã đăng nhập đều xem được.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SectionResponse>>> getSections(@PathVariable Long courseId) {
        List<SectionOutput> outputs = getSectionsUseCase.execute(courseId);
        List<SectionResponse> responses = outputs.stream()
                .map(SectionResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * POST /api/v1/courses/{courseId}/sections
     * Tạo section mới. Chỉ INSTRUCTOR (course của mình), STAFF, SUPER_ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SectionResponse>> createSection(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateSectionRequest req,
            HttpServletRequest request) {

        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);
        Role requesterRole = Role.reconstitute(null, claims.get("role", String.class), null);

        CreateSectionInput input = new CreateSectionInput(
                courseId, requesterId, requesterRole, req.getTitle(), req.getOrderIndex());

        SectionOutput output = createSectionUseCase.execute(input);
        return ResponseEntity.status(201).body(ApiResponse.created(SectionResponse.from(output)));
    }

    /**
     * PUT /api/v1/courses/{courseId}/sections/{sectionId}
     * Cập nhật section. Chỉ INSTRUCTOR (course của mình), STAFF, SUPER_ADMIN.
     */
    @PutMapping("/{sectionId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SectionResponse>> updateSection(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @Valid @RequestBody UpdateSectionRequest req,
            HttpServletRequest request) {

        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);
        Role requesterRole = Role.reconstitute(null, claims.get("role", String.class), null);

        UpdateSectionInput input = new UpdateSectionInput(
                sectionId, courseId, requesterId, requesterRole, req.getTitle(), req.getOrderIndex());

        SectionOutput output = updateSectionUseCase.execute(input);
        return ResponseEntity.ok(ApiResponse.success("Updated", SectionResponse.from(output)));
    }

    /**
     * DELETE /api/v1/courses/{courseId}/sections/{sectionId}
     * Xóa section (cascade xóa lessons). Chỉ INSTRUCTOR (course của mình), STAFF, SUPER_ADMIN.
     */
    @DeleteMapping("/{sectionId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSection(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            HttpServletRequest request) {

        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);
        Role requesterRole = Role.reconstitute(null, claims.get("role", String.class), null);

        DeleteSectionInput input = new DeleteSectionInput(sectionId, courseId, requesterId, requesterRole);
        deleteSectionUseCase.execute(input);

        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}
