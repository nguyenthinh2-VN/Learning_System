package com.example.learning_system_spring.infrastructure.config;

import com.example.learning_system_spring.application.repository.RolePermissionRepository;
import com.example.learning_system_spring.domain.model.RolePermissions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Cache in-memory ánh xạ roleName → tập permission (Phương án C — Hybrid).
 *
 * - Nạp một lần khi app sẵn sàng và mỗi khi ma trận thay đổi ({@link #reload()}).
 * - {@link JwtAuthenticationFilter} đọc cache này (O(1)) để set authorities mỗi request,
 *   nhờ vậy thay đổi phân quyền có hiệu lực gần như tức thì mà không phình JWT.
 *
 * Lưu ý: cache là per-instance. Khi chạy nhiều instance cần cơ chế refresh phân tán
 * (pub/sub hoặc TTL) — ngoài phạm vi MVP, xem ghi chú trong plan.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    private final RolePermissionRepository rolePermissionRepository;

    private volatile Map<String, Set<String>> cache = Collections.emptyMap();

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        reload();
    }

    /** Nạp lại toàn bộ ma trận từ DB vào cache. Gọi sau mỗi lần admin đổi quyền. */
    public void reload() {
        try {
            Map<String, Set<String>> fresh = new HashMap<>();
            for (RolePermissions rp : rolePermissionRepository.findMatrix()) {
                fresh.put(rp.getRoleName().toUpperCase(), new LinkedHashSet<>(rp.getPermissionNames()));
            }
            this.cache = fresh;
            log.info("Permission cache reloaded for {} roles", fresh.size());
        } catch (Exception e) {
            log.error("Failed to reload permission cache", e);
        }
    }

    /** Tập permission của một role; trả về set rỗng nếu role chưa có trong cache. */
    public Set<String> getPermissions(String roleName) {
        if (roleName == null) return Collections.emptySet();
        Set<String> perms = cache.get(roleName.toUpperCase());
        return perms == null ? Collections.emptySet() : perms;
    }
}
