package com.example.learning_system_spring.infrastructure.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseLedgerService {

    private final ObjectMapper objectMapper;
    private static final String LEDGER_FILE = "logs/purchase_ledger.jsonl";
    private final Object lock = new Object();

    public void logPurchase(Long userId, Long courseId, BigDecimal paidPrice, LocalDateTime timestamp) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("event", "PURCHASE_COMPLETED");
        record.put("userId", userId);
        record.put("courseId", courseId);
        record.put("paidPrice", paidPrice);
        record.put("timestamp", timestamp.toString());
        append(record);
    }

    public void logVoucherApplied(Long userId, Long courseId, Long voucherId, String voucherCode,
                                  BigDecimal originalPrice, BigDecimal discountAmount, BigDecimal finalPrice,
                                  Long enrollmentId, LocalDateTime appliedAt) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("event", "VOUCHER_APPLIED");
        record.put("userId", userId);
        record.put("courseId", courseId);
        record.put("voucherId", voucherId);
        record.put("voucherCode", voucherCode);
        record.put("originalPrice", originalPrice);
        record.put("discountAmount", discountAmount);
        record.put("finalPrice", finalPrice);
        record.put("enrollmentId", enrollmentId);
        record.put("appliedAt", appliedAt.toString());
        append(record);
    }

    public void logVoucherRejected(Long userId, Long courseId, String voucherCode, String reason) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("event", "VOUCHER_REJECTED");
        record.put("userId", userId);
        record.put("courseId", courseId);
        record.put("voucherCode", voucherCode);
        record.put("reason", reason);
        record.put("timestamp", LocalDateTime.now().toString());
        append(record);
    }

    private void append(Map<String, Object> record) {
        try {
            Path path = Paths.get(LEDGER_FILE);
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            String jsonLine = objectMapper.writeValueAsString(record) + System.lineSeparator();
            synchronized (lock) {
                Files.writeString(path, jsonLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            log.error("Failed to write to purchase ledger: {}", record, e);
        }
    }
}
