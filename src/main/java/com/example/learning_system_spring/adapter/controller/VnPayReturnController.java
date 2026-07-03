package com.example.learning_system_spring.adapter.controller;

import com.example.learning_system_spring.adapter.dto.response.ApiResponse;
import com.example.learning_system_spring.application.dto.Wallet.TopUpStatusOutput;
import com.example.learning_system_spring.application.usecase.Wallet.GetTopUpStatusUseCase;
import com.example.learning_system_spring.infrastructure.config.JwtService;
import com.example.learning_system_spring.infrastructure.payment.VnPayTopUpProcessor;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint cho luồng VNPay phía user (yêu cầu đăng nhập) — chỉ active khi payment.provider=vnpay.
 *
 * Tách khỏi WalletController vì VnPayTopUpProcessor là bean có điều kiện (chỉ tồn tại ở vnpay mode);
 * inject thẳng vào WalletController sẽ làm hỏng mock mode.
 */
@RestController
@RequestMapping("/api/v1/wallet/top-up")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.provider", havingValue = "vnpay")
public class VnPayReturnController {

    private final VnPayTopUpProcessor processor;
    private final GetTopUpStatusUseCase getTopUpStatusUseCase;
    private final JwtService jwtService;

    /**
     * Kênh RETURN — trang FE gửi toàn bộ vnp_* để BE verify chữ ký + đối soát + hoàn tất.
     * Chỉ hoàn tất khi vnpay.complete-mode=RETURN; ngược lại chỉ ghi nhận (IPN sẽ cộng).
     */
    @PostMapping("/vnpay-return")
    public ResponseEntity<ApiResponse<TopUpStatusOutput>> vnpayReturn(
            @RequestBody Map<String, String> params,
            HttpServletRequest httpRequest) {

        Long userId = parseClaims(httpRequest).get("userId", Long.class);
        VnPayTopUpProcessor.Result result = processor.processReturn(params, userId);

        TopUpStatusOutput body = new TopUpStatusOutput(
                result.referenceCode(), result.status(), result.amount());

        return switch (result.outcome()) {
            case SUCCESS, ALREADY_COMPLETED, ACKNOWLEDGED ->
                    ResponseEntity.ok(ApiResponse.success("Xử lý kết quả thanh toán thành công", body));
            case PAYMENT_FAILED ->
                    ResponseEntity.ok(ApiResponse.success("Thanh toán không thành công", body));
            case INVALID_SIGNATURE ->
                    ResponseEntity.status(400).body(ApiResponse.error(400, "INVALID_SIGNATURE", "Chữ ký không hợp lệ"));
            case TX_NOT_FOUND ->
                    ResponseEntity.status(404).body(ApiResponse.error(404, "TX_NOT_FOUND", "Không tìm thấy giao dịch"));
            case AMOUNT_MISMATCH ->
                    ResponseEntity.status(400).body(ApiResponse.error(400, "AMOUNT_MISMATCH", "Số tiền không khớp giao dịch"));
        };
    }

    /**
     * Đọc trạng thái giao dịch (FE poll khi complete-mode=IPN). Không cộng tiền.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<TopUpStatusOutput>> status(
            @RequestParam("ref") String ref,
            HttpServletRequest httpRequest) {

        Long userId = parseClaims(httpRequest).get("userId", Long.class);
        TopUpStatusOutput output = getTopUpStatusUseCase.execute(ref, userId);
        return ResponseEntity.ok(ApiResponse.success(output));
    }

    private Claims parseClaims(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtService.parseToken(token);
    }
}
