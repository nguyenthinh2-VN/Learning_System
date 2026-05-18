package com.example.learning_system_spring.infrastructure.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseLedgerService {

    private final ObjectMapper objectMapper;
    private static final String LEDGER_FILE = "logs/purchase_ledger.jsonl";
    private final Object lock = new Object();

    public void logPurchase(Long userId, Long courseId, java.math.BigDecimal paidPrice,
            java.time.LocalDateTime timestamp) {
        try {
            Path path = Paths.get(LEDGER_FILE);
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }

            Map<String, Object> record = Map.of(
                    "userId", userId,
                    "courseId", courseId,
                    "paidPrice", paidPrice,
                    "timestamp", timestamp.toString());

            String jsonLine = objectMapper.writeValueAsString(record) + System.lineSeparator();

            synchronized (lock) {
                Files.writeString(path, jsonLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            log.error("Failed to write to purchase ledger for user {} course {}", userId, courseId, e);
            // In a real system we might alert or throw, but here we just log so it doesn't
            // rollback the main transaction
        }
    }
}
