package com.example.learning_system_spring.infrastructure.payment;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình VNPay — chỉ bind khi payment.provider=vnpay.
 *
 * Relaxed binding: tmn-code → tmnCode, hash-secret → hashSecret, ...
 * Gom về một chỗ để VnPayGateway + VnPayController + VnPayWebhookController dùng chung,
 * tránh lặp @Value.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "vnpay")
@ConditionalOnProperty(name = "payment.provider", havingValue = "vnpay")
public class VnPayProperties {

    /** Mã website (merchant) do VNPay cấp. */
    private String tmnCode;

    /** Secret để ký HMAC-SHA512. KHÔNG commit secret thật. */
    private String hashSecret;

    /** URL cổng thanh toán sandbox/production. */
    private String payUrl;

    /** URL trình duyệt được redirect về sau khi thanh toán (trang FE). */
    private String returnUrl;

    /** URL IPN server→server (chỉ dùng ở production / khi complete-mode=IPN). */
    private String ipnUrl;

    /** RETURN (dev: hoàn tất tại trang return) hoặc IPN (production: hoàn tất qua webhook). */
    private String completeMode = "RETURN";

    /** Thời gian sống của URL thanh toán (phút). */
    private int ttlMinutes = 15;

    public boolean isReturnMode() {
        return "RETURN".equalsIgnoreCase(completeMode);
    }

    public boolean isIpnMode() {
        return "IPN".equalsIgnoreCase(completeMode);
    }
}
