package com.example.learning_system_spring.infrastructure.config;

import com.example.learning_system_spring.domain.service.PricingEngine;
import com.example.learning_system_spring.domain.service.VoucherValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Đăng ký các pure domain service (không có Spring annotation) làm Spring bean
 * để có thể inject vào use case. Giữ domain layer "thuần" — không phụ thuộc Spring.
 */
@Configuration
public class DomainServiceConfig {

    @Bean
    public PricingEngine pricingEngine() {
        return new PricingEngine();
    }

    @Bean
    public VoucherValidator voucherValidator() {
        return new VoucherValidator();
    }
}
