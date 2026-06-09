package com.example.learning_system_spring.infrastructure.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Audit log cho thay đổi ma trận phân quyền (chốt 2.5). Ghi JSONL, tái dùng pattern
 * của {@link PurchaseLedgerService}. Mỗi thay đổi ma trận ghi 1 dòng gồm: ai, role nào,
 * permission trước/sau, các permission được thêm/gỡ, thời điểm.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionAuditService {

    private final ObjectMapper objectMapper;
    private static final String AUDIT_FILE = "logs/permission_audit.jsonl";
    private final Object lock = new Object();

    public void logRolePermissionsUpdated(Long actorId, String roleName,
                                          List<String> before, List<String> after,
                                          List<String> granted, List<String> revoked) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("event", "ROLE_PERMISSIONS_UPDATED");
        record.put("actorId", actorId);
        record.put("roleName", roleName);
        record.put("before", before);
        record.put("after", after);
        record.put("granted", granted);
        record.put("revoked", revoked);
        record.put("timestamp", LocalDateTime.now().toString());
        append(record);
    }

    private void append(Map<String, Object> record) {
        try {
            Path path = Paths.get(AUDIT_FILE);
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            String jsonLine = objectMapper.writeValueAsString(record) + System.lineSeparator();
            synchronized (lock) {
                Files.writeString(path, jsonLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            log.error("Failed to write to permission audit ledger: {}", record, e);
        }
    }
}
