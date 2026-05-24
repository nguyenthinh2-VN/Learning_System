package com.example.learning_system_spring.adapter.controller.Course;

import com.example.learning_system_spring.adapter.dto.request.Course.PurchaseCourseRequest;
import com.example.learning_system_spring.adapter.dto.response.QuotePricingResponse;
import com.example.learning_system_spring.application.dto.Voucher.QuotePricingInput;
import com.example.learning_system_spring.application.dto.Voucher.QuotePricingOutput;
import com.example.learning_system_spring.application.usecase.Voucher.QuotePricingUseCase;
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

/**
 * Endpoint tính giá xem trước (preview / quote). Read-only, không tiêu thụ voucher.
 *
 * Anti-tampering: chỉ nhận courseId từ path + voucherCode từ body.
 * Mọi field giá khác trong body request bị Spring bỏ qua (DTO không khai báo).
 */
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseQuoteController {

    private final QuotePricingUseCase quotePricingUseCase;
    private final JwtService jwtService;

    @PostMapping("/{courseId}/quote")
    public ResponseEntity<?> quote(@PathVariable Long courseId,
                                   @Valid @RequestBody(required = false) PurchaseCourseRequest req,
                                   HttpServletRequest request) {
        Claims claims = parseClaims(request);
        Long requesterId = claims.get("userId", Long.class);
        Role requesterRole = Role.reconstitute(null, claims.get("role", String.class), null);
        Boolean isInternal = claims.get("isInternal", Boolean.class);

        String voucherCode = req != null ? req.getVoucherCode() : null;
        QuotePricingInput input = new QuotePricingInput(
                courseId, voucherCode, requesterId, requesterRole,
                Boolean.TRUE.equals(isInternal));

        QuotePricingOutput output = quotePricingUseCase.execute(input);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Success",
                "data", QuotePricingResponse.from(output),
                "timestamp", LocalDateTime.now()));
    }

    private Claims parseClaims(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtService.parseToken(token);
    }
}
