package com.example.learning_system_spring.adapter.controller.Course;

import com.example.learning_system_spring.adapter.dto.response.CourseDetailResponse;
import com.example.learning_system_spring.adapter.dto.response.CourseListResponse;
import com.example.learning_system_spring.application.dto.Course.GetCourseDetailInput;
import com.example.learning_system_spring.application.dto.Course.GetCourseListInput;
import com.example.learning_system_spring.application.usecase.Course.GetCourseDetailUseCase;
import com.example.learning_system_spring.application.usecase.Course.GetCourseListUseCase;
import com.example.learning_system_spring.application.usecase.Course.CreateCourseUseCase;
import com.example.learning_system_spring.application.usecase.Course.UpdateCourseUseCase;
import com.example.learning_system_spring.application.usecase.Course.DeleteCourseUseCase;
import com.example.learning_system_spring.application.dto.Course.CreateCourseInput;
import com.example.learning_system_spring.application.dto.Course.UpdateCourseInput;
import com.example.learning_system_spring.application.dto.Course.DeleteCourseInput;
import com.example.learning_system_spring.application.dto.Course.CourseOutput;
import com.example.learning_system_spring.application.dto.Voucher.PurchaseCourseInput;
import com.example.learning_system_spring.application.dto.Voucher.PurchaseCourseOutput;
import com.example.learning_system_spring.application.usecase.Voucher.ApplyVoucherCheckoutUseCase;
import com.example.learning_system_spring.adapter.dto.request.Course.CreateCourseRequest;
import com.example.learning_system_spring.adapter.dto.request.Course.PurchaseCourseRequest;
import com.example.learning_system_spring.adapter.dto.request.Course.UpdateCourseRequest;
import com.example.learning_system_spring.adapter.dto.response.PurchaseCourseResponse;
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
        private final ApplyVoucherCheckoutUseCase applyVoucherCheckoutUseCase;
        private final JwtService jwtService;

        private Claims getClaims(HttpServletRequest request) {
                String token = request.getHeader("Authorization").substring(7);
                return jwtService.parseToken(token);
        }

        /**
         * Lấy claims nếu request có Authorization header, ngược lại trả null (anonymous).
         */
        private Claims getClaimsOptional(HttpServletRequest request) {
                String header = request.getHeader("Authorization");
                if (header == null || !header.startsWith("Bearer ")) {
                        return null;
                }
                try {
                        return jwtService.parseToken(header.substring(7));
                } catch (Exception e) {
                        return null;
                }
        }

        @GetMapping
        public ResponseEntity<?> getCourses(
                        @RequestParam(defaultValue = "") String keyword,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                // Public listing: chỉ trả course đã publish.
                GetCourseListInput input = GetCourseListInput.publicScope(keyword, page, size);
                var output = getCourseListUseCase.execute(input);

                return ResponseEntity.ok(Map.of(
                                "status", 200,
                                "message", "Success",
                                "data", CourseListResponse.from(output),
                                "timestamp", LocalDateTime.now()));
        }

        @GetMapping("/{id}")
        public ResponseEntity<?> getCourseDetail(@PathVariable Long id, HttpServletRequest request) {
                // Có thể là anonymous hoặc đăng nhập. Owner / admin xem được course chưa publish.
                Claims claims = getClaimsOptional(request);
                Long requesterId = claims != null ? claims.get("userId", Long.class) : null;
                Role requesterRole = claims != null
                                ? Role.reconstitute(null, claims.get("role", String.class), null)
                                : null;

                GetCourseDetailInput input = new GetCourseDetailInput(id, requesterId, requesterRole);
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
                                "message", "Created (chưa public, chờ admin duyệt)",
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
        public ResponseEntity<?> purchaseCourse(@PathVariable Long id,
                                                @Valid @RequestBody(required = false) PurchaseCourseRequest req,
                                                HttpServletRequest request) {
                Claims claims = getClaims(request);
                Long requesterId = claims.get("userId", Long.class);
                Role requesterRole = Role.reconstitute(null, claims.get("role", String.class), null);
                Boolean isInternal = claims.get("isInternal", Boolean.class);

                String voucherCode = req != null ? req.getVoucherCode() : null;
                PurchaseCourseInput input = new PurchaseCourseInput(
                                id, voucherCode, requesterId, requesterRole,
                                Boolean.TRUE.equals(isInternal));

                PurchaseCourseOutput output = applyVoucherCheckoutUseCase.execute(input);

                return ResponseEntity.ok(Map.of(
                                "status", 200,
                                "message", "Đăng ký khóa học thành công",
                                "data", PurchaseCourseResponse.from(output),
                                "timestamp", LocalDateTime.now()));
        }
}
