package com.example.learning_system_spring.adapter.controller.Cart;

import com.example.learning_system_spring.application.dto.Cart.AddCourseToCartInput;
import com.example.learning_system_spring.application.dto.Cart.BulkCheckoutInput;
import com.example.learning_system_spring.application.dto.Cart.BulkCheckoutOutput;
import com.example.learning_system_spring.application.dto.Cart.CartOutput;
import com.example.learning_system_spring.application.usecase.Cart.AddCourseToCartUseCase;
import com.example.learning_system_spring.application.usecase.Cart.BulkCheckoutUseCase;
import com.example.learning_system_spring.application.usecase.Cart.GetCartItemsUseCase;
import com.example.learning_system_spring.application.usecase.Cart.RemoveCourseFromCartUseCase;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.infrastructure.config.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CartController {

    private final AddCourseToCartUseCase addCourseToCartUseCase;
    private final RemoveCourseFromCartUseCase removeCourseFromCartUseCase;
    private final GetCartItemsUseCase getCartItemsUseCase;
    private final BulkCheckoutUseCase bulkCheckoutUseCase;
    private final JwtService jwtService;

    private Claims getClaims(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtService.parseToken(token);
    }

    @GetMapping("/cart")
    public ResponseEntity<?> getCart(HttpServletRequest request) {
        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);

        CartOutput output = getCartItemsUseCase.execute(requesterId);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Success",
                "data", output,
                "timestamp", LocalDateTime.now()));
    }

    @PostMapping("/cart/items")
    public ResponseEntity<?> addItemsToCart(@RequestBody Map<String, List<Long>> req, HttpServletRequest request) {
        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);

        List<Long> courseIds = req.get("course_ids");
        if (courseIds == null || courseIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "course_ids is required"));
        }

        AddCourseToCartInput input = new AddCourseToCartInput(requesterId, courseIds);
        addCourseToCartUseCase.execute(input);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Đã thêm vào giỏ hàng",
                "timestamp", LocalDateTime.now()));
    }

    @DeleteMapping("/cart/items/{courseId}")
    public ResponseEntity<?> removeItem(@PathVariable Long courseId, HttpServletRequest request) {
        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);

        removeCourseFromCartUseCase.remove(requesterId, courseId);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Đã xóa khỏi giỏ hàng",
                "timestamp", LocalDateTime.now()));
    }

    @DeleteMapping("/cart/items")
    public ResponseEntity<?> clearCart(HttpServletRequest request) {
        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);

        removeCourseFromCartUseCase.clearCart(requesterId);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Đã làm trống giỏ hàng",
                "timestamp", LocalDateTime.now()));
    }

    @PostMapping("/checkout/bulk")
    public ResponseEntity<?> bulkCheckout(@RequestBody Map<String, Object> req, HttpServletRequest request) {
        Claims claims = getClaims(request);
        Long requesterId = claims.get("userId", Long.class);
        Role requesterRole = Role.reconstitute(null, claims.get("role", String.class), null);
        Boolean isInternal = claims.get("isInternal", Boolean.class);

        List<Integer> courseIdsInt = (List<Integer>) req.get("course_ids");
        if (courseIdsInt == null || courseIdsInt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "course_ids is required"));
        }
        
        List<Long> courseIds = courseIdsInt.stream().map(Integer::longValue).toList();
        String voucherCode = (String) req.get("voucher_code");

        BulkCheckoutInput input = new BulkCheckoutInput(
                requesterId, requesterRole, Boolean.TRUE.equals(isInternal), courseIds, voucherCode
        );

        BulkCheckoutOutput output = bulkCheckoutUseCase.execute(input);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Thanh toán thành công",
                "data", output,
                "timestamp", LocalDateTime.now()));
    }
}
