package com.example.learning_system_spring.adapter.controller;

import com.example.learning_system_spring.adapter.dto.request.Voucher.CreateVoucherRequest;
import com.example.learning_system_spring.adapter.dto.request.Voucher.UpdateVoucherRequest;
import com.example.learning_system_spring.adapter.dto.response.VoucherResponse;
import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.application.dto.Voucher.CreateVoucherInput;
import com.example.learning_system_spring.application.dto.Voucher.DeleteVoucherInput;
import com.example.learning_system_spring.application.dto.Voucher.GetVouchersInput;
import com.example.learning_system_spring.application.dto.Voucher.UpdateVoucherInput;
import com.example.learning_system_spring.application.dto.Voucher.VoucherOutput;
import com.example.learning_system_spring.application.usecase.Voucher.CreateVoucherUseCase;
import com.example.learning_system_spring.application.usecase.Voucher.DeleteVoucherUseCase;
import com.example.learning_system_spring.application.usecase.Voucher.GetVouchersUseCase;
import com.example.learning_system_spring.application.usecase.Voucher.UpdateVoucherUseCase;
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
@RequestMapping("/api/v1/admin/vouchers")
@RequiredArgsConstructor
public class AdminVoucherController {

    private final CreateVoucherUseCase createVoucherUseCase;
    private final UpdateVoucherUseCase updateVoucherUseCase;
    private final DeleteVoucherUseCase deleteVoucherUseCase;
    private final GetVouchersUseCase getVouchersUseCase;
    private final JwtService jwtService;

    private Claims getClaims(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtService.parseToken(token);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateVoucherRequest req, HttpServletRequest request) {
        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);
        Role requesterRole = Role.reconstitute(null, claims.get("role", String.class), null);

        CreateVoucherInput input = new CreateVoucherInput(
                requesterId, requesterRole,
                req.getCode(), req.getType(), req.getValue(),
                req.getScope(), req.getValidFrom(), req.getValidTo(),
                req.getMinOrderAmount(), req.getMaxDiscount(),
                req.getUsageLimit(), req.getUsagePerUser(),
                req.getApplicableCourseIds());

        VoucherOutput output = createVoucherUseCase.execute(input);

        return ResponseEntity.status(201).body(Map.of(
                "status", 201,
                "message", "Tạo voucher thành công",
                "data", VoucherResponse.from(output),
                "timestamp", LocalDateTime.now()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @Valid @RequestBody UpdateVoucherRequest req,
                                    HttpServletRequest request) {
        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);
        Role requesterRole = Role.reconstitute(null, claims.get("role", String.class), null);

        UpdateVoucherInput input = new UpdateVoucherInput(
                id, requesterId, requesterRole,
                req.getCode(), req.getType(), req.getValue(),
                req.getStatus(), req.getScope(), req.getValidFrom(), req.getValidTo(),
                req.getMinOrderAmount(), req.getMaxDiscount(),
                req.getUsageLimit(), req.getUsagePerUser(),
                req.getApplicableCourseIds());

        VoucherOutput output = updateVoucherUseCase.execute(input);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Cập nhật voucher thành công",
                "data", VoucherResponse.from(output),
                "timestamp", LocalDateTime.now()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);
        Role requesterRole = Role.reconstitute(null, claims.get("role", String.class), null);

        deleteVoucherUseCase.execute(new DeleteVoucherInput(id, requesterId, requesterRole));

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Voucher đã được vô hiệu hóa (soft-delete)",
                "timestamp", LocalDateTime.now()));
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  HttpServletRequest request) {
        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);
        Role requesterRole = Role.reconstitute(null, claims.get("role", String.class), null);

        PageResult<VoucherOutput> page1 = getVouchersUseCase.execute(
                new GetVouchersInput(page, size, requesterId, requesterRole));

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Success",
                "data", Map.of(
                        "totalElements", page1.totalElements(),
                        "totalPages", page1.totalPages(),
                        "page", page1.page(),
                        "size", page1.size(),
                        "items", page1.items().stream().map(VoucherResponse::from).toList()
                ),
                "timestamp", LocalDateTime.now()));
    }
}
