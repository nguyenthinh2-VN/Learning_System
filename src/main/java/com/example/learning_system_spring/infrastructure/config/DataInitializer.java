package com.example.learning_system_spring.infrastructure.config;

import com.example.learning_system_spring.adapter.repository.jpa.role_permissionEntity.PermissionJpaEntity;
import com.example.learning_system_spring.adapter.repository.jpa.role_permissionEntity.RoleJpaEntity;
import com.example.learning_system_spring.domain.model.Permission;
import com.example.learning_system_spring.domain.model.Role;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final EntityManager em;

    @Override
    @Transactional
    public void run(String... args) {
        initRoles();
        initPermissions();
    }

    private void initRoles() {
        Long count = em.createQuery("SELECT COUNT(r) FROM RoleJpaEntity r", Long.class).getSingleResult();
        if (count > 0) return;

        em.persist(RoleJpaEntity.fromDomain(Role.create("MEMBER", "Người dùng thông thường - mặc định khi đăng ký")));
        em.persist(RoleJpaEntity.fromDomain(Role.create("STAFF", "Nhân viên - có quyền quản lý nội dung")));
        em.persist(RoleJpaEntity.fromDomain(Role.create("ADMIN", "Quản trị viên - toàn quyền hệ thống")));

        log.info("Seeded 3 roles: MEMBER, STAFF, ADMIN");
    }

    private void initPermissions() {
        Long count = em.createQuery("SELECT COUNT(p) FROM PermissionJpaEntity p", Long.class).getSingleResult();
        if (count > 0) return;

        em.persist(PermissionJpaEntity.fromDomain(Permission.create("VIEW_COURSE", "Xem khóa học")));
        em.persist(PermissionJpaEntity.fromDomain(Permission.create("ENROLL_COURSE", "Đăng ký khóa học")));
        em.persist(PermissionJpaEntity.fromDomain(Permission.create("CREATE_COURSE", "Tạo khóa học mới")));
        em.persist(PermissionJpaEntity.fromDomain(Permission.create("EDIT_COURSE", "Chỉnh sửa khóa học")));
        em.persist(PermissionJpaEntity.fromDomain(Permission.create("DELETE_COURSE", "Xóa khóa học")));
        em.persist(PermissionJpaEntity.fromDomain(Permission.create("CREATE_SECTION", "Tạo chương học trong khóa học")));
        em.persist(PermissionJpaEntity.fromDomain(Permission.create("EDIT_SECTION", "Sửa / Xóa chương học")));
        em.persist(PermissionJpaEntity.fromDomain(Permission.create("VIEW_USER", "Xem thông tin người dùng")));
        em.persist(PermissionJpaEntity.fromDomain(Permission.create("EDIT_USER", "Chỉnh sửa người dùng")));
        em.persist(PermissionJpaEntity.fromDomain(Permission.create("DELETE_USER", "Xóa người dùng")));
        em.persist(PermissionJpaEntity.fromDomain(Permission.create("MANAGE_ROLE", "Quản lý phân quyền")));
        em.persist(PermissionJpaEntity.fromDomain(Permission.create("VIEW_REPORT", "Xem báo cáo thống kê")));

        log.info("Seeded 12 permissions");
    }
}
