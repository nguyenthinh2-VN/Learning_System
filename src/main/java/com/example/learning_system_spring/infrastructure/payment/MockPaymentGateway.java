package com.example.learning_system_spring.infrastructure.payment;

import com.example.learning_system_spring.application.port.PaymentGateway;
import com.example.learning_system_spring.application.port.PaymentInitResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mock payment gateway — chỉ active khi payment.provider=mock (môi trường dev).
 *
 * Không gọi bất kỳ API bên ngoài nào.
 * Dùng POST /api/v1/webhook/mock?ref={referenceCode} để giả lập thanh toán thành công.
 */
@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PaymentInitResult initPayment(String referenceCode, BigDecimal amount) {
        String guide = "Gọi POST /api/v1/webhook/mock?ref=" + referenceCode
                + " để giả lập thanh toán thành công (chỉ dùng trong môi trường dev)";
        return new PaymentInitResult(
                referenceCode,
                amount,
                guide,
                "MESSAGE",
                LocalDateTime.now().plusMinutes(15)
        );
    }

    @Override
    public String providerName() {
        return "MOCK";
    }
}
