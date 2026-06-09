package com.example.learning_system_spring.adapter.controller;

import com.example.learning_system_spring.infrastructure.payment.VnPayTopUpProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * IPN endpoint VNPay (server→server) — chỉ active khi payment.provider=vnpay.
 *
 * VNPay gọi GET với toàn bộ vnp_* trên query string. Trả JSON {RspCode, Message} theo chuẩn
 * để VNPay biết đã nhận. Chỉ thực sự cộng tiền khi complete-mode=IPN (production).
 *
 * Public (SecurityConfig đã permitAll /api/v1/webhook/**) — bảo mật bằng verify chữ ký HMAC.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.provider", havingValue = "vnpay")
public class VnPayWebhookController {

    private final VnPayTopUpProcessor processor;

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> ipn(@RequestParam Map<String, String> params) {
        log.info("[VNPay-IPN] ref={} responseCode={}",
                params.get("vnp_TxnRef"), params.get("vnp_ResponseCode"));

        VnPayTopUpProcessor.Result result = processor.processIpn(params);

        return switch (result.outcome()) {
            case SUCCESS, ACKNOWLEDGED ->
                    ResponseEntity.ok(rsp("00", "Confirm Success"));
            case ALREADY_COMPLETED ->
                    ResponseEntity.ok(rsp("02", "Order already confirmed"));
            case PAYMENT_FAILED ->
                    // Vẫn trả 00 để VNPay ngừng retry; tiền không được cộng.
                    ResponseEntity.ok(rsp("00", "Confirm Success"));
            case INVALID_SIGNATURE ->
                    ResponseEntity.ok(rsp("97", "Invalid Checksum"));
            case TX_NOT_FOUND ->
                    ResponseEntity.ok(rsp("01", "Order not Found"));
            case AMOUNT_MISMATCH ->
                    ResponseEntity.ok(rsp("04", "Invalid Amount"));
        };
    }

    private Map<String, String> rsp(String code, String message) {
        return Map.of("RspCode", code, "Message", message);
    }
}
