package com.example.learning_system_spring.adapter.controller.Course;

import com.example.learning_system_spring.adapter.dto.response.CourseDetailResponse;
import com.example.learning_system_spring.adapter.dto.response.CourseListResponse;
import com.example.learning_system_spring.application.dto.Course.GetCourseDetailInput;
import com.example.learning_system_spring.application.dto.Course.GetCourseListInput;
import com.example.learning_system_spring.application.usecase.Course.GetCourseDetailUseCase;
import com.example.learning_system_spring.application.usecase.Course.GetCourseListUseCase;
import com.example.learning_system_spring.application.usecase.Course.GetCourseListUseCase;
import com.example.learning_system_spring.application.usecase.Course.CreateCourseUseCase;
import com.example.learning_system_spring.application.usecase.Course.UpdateCourseUseCase;
import com.example.learning_system_spring.application.usecase.Course.DeleteCourseUseCase;
import com.example.learning_system_spring.application.dto.Course.CreateCourseInput;
import com.example.learning_system_spring.application.dto.Course.UpdateCourseInput;
import com.example.learning_system_spring.application.dto.Course.DeleteCourseInput;
import com.example.learning_system_spring.application.dto.Course.CourseOutput;
import com.example.learning_system_spring.application.usecase.Course.PurchaseCourseUseCase;
import com.example.learning_system_spring.adapter.dto.request.Course.CreateCourseRequest;
import com.example.learning_system_spring.adapter.dto.request.Course.UpdateCourseRequest;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.infrastructure.config.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

        private final GetCourseListUseCase getCourseListUseCase;
        private final GetCourseDetailUseCase getCourseDetailUseCase;
        private final CreateCourseUseCase createCourseUseCase;
        private final UpdateCourseUseCase updateCourseUseCase;
        private final DeleteCourseUseCase deleteCourseUseCase;
        private final PurchaseCourseUseCase purchaseCourseUseCase;
        private final JwtService jwtService;

        private Claims getClaims(HttpServletRequest request) {
                String token = request.getHeader("Authorization").substring(7);
                return jwtService.parseToken(token);
        }

        @GetMapping
        public ResponseEntity<?> getCourses(
                        @RequestParam(defaultValue = "") String keyword,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                GetCourseListInput input = new GetCourseListInput(keyword, page, size);
                var output = getCourseListUseCase.execute(input);

                return ResponseEntity.ok(Map.of(
                                "status", 200,
                                "message", "Success",
                                "data", CourseListResponse.from(output),
                                "timestamp", LocalDateTime.now()));
        }

        @GetMapping("/{id}")
        public ResponseEntity<?> getCourseDetail(@PathVariable Long id) {
                GetCourseDetailInput input = new GetCourseDetailInput(id);
                CourseOutput output = getCourseDetailUseCase.execute(input);

                return ResponseEntity.ok(Map.of(
                                "status", 200,
                                "message", "Success",
                                "data", CourseDetailResponse.from(output),
                                "timestamp", LocalDateTime.now()));
        }

        @PostMapping
        public ResponseEntity<?> createCourse(@Valid @RequestBody CreateCourseRequest req, HttpServletRequest request) {
                Claims claims = getClaims(request);
                Long requesterId = claims.get("userId", Long.class);
                Role requesterRole = Role.reconstitute(null, claims.get("role", String.class), null);

                CreateCourseInput input = new CreateCourseInput(
                                requesterId, requesterRole, req.getTitle(), req.getDescription(),
                                req.getMaxStudents(), req.getPrice(), req.getRequestedInstructorId(),
                                req.getSections());

                CourseOutput output = createCourseUseCase.execute(input);

                return ResponseEntity.status(201).body(Map.of(
                                "status", 201,
                                "message", "Created",
                                "data", output,
                                "timestamp", LocalDateTime.now()));
        }

        @PutMapping("/{id}")
        public ResponseEntity<?> updateCourse(@PathVariable Long id, @Valid @RequestBody UpdateCourseRequest req,
                        HttpServletRequest request) {
                Claims claims = getClaims(request);
                Long requesterId = claims.get("userId", Long.class);
                Role requesterRole = Role.reconstitute(null, claims.get("role", String.class), null);

                UpdateCourseInput input = new UpdateCourseInput(
                                id, requesterId, requesterRole, req.getTitle(), req.getDescription(),
                                req.getMaxStudents(), req.getPrice(), req.getSections());

                CourseOutput output = updateCourseUseCase.execute(input);

                return ResponseEntity.ok(Map.of(
                                "status", 200,
                                "message", "Updated",
                                "data", output,
                                "timestamp", LocalDateTime.now()));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<?> deleteCourse(@PathVariable Long id, HttpServletRequest request) {
                Claims claims = getClaims(request);
                Long requesterId = claims.get("userId", Long.class);
                Role requesterRole = Role.reconstitute(null, claims.get("role", String.class), null);

                DeleteCourseInput input = new DeleteCourseInput(id, requesterId, requesterRole);
                deleteCourseUseCase.execute(input);

                return ResponseEntity.ok(Map.of(
                                "status", 200,
                                "message", "Deleted",
                                "timestamp", LocalDateTime.now()));
        }

        @PostMapping("/{id}/purchase")
        public ResponseEntity<?> purchaseCourse(@PathVariable Long id, HttpServletRequest request) {
                Claims claims = getClaims(request);
                Long requesterId = claims.get("userId", Long.class);

                var enrollment = purchaseCourseUseCase.execute(requesterId, id);

                return ResponseEntity.ok(Map.of(
                                "status", 200,
                                "message", "Đăng ký khóa học thành công",
                                "data",
                                Map.of("enrollmentId", enrollment.getId(), "paidPrice", enrollment.getPaidPrice()),
                                "timestamp", LocalDateTime.now()));
        }
}
