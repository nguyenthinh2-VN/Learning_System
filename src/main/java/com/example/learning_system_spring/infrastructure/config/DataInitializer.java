package com.example.learning_system_spring.infrastructure.config;

import com.example.learning_system_spring.adapter.repository.jpa.UserEntity.UserJpaEntity;
import com.example.learning_system_spring.adapter.repository.jpa.role_permissionEntity.PermissionJpaEntity;
import com.example.learning_system_spring.adapter.repository.jpa.role_permissionEntity.RoleJpaEntity;
import com.example.learning_system_spring.domain.model.Permission;
import com.example.learning_system_spring.domain.model.Role;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final EntityManager em;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        initRoles();
        initPermissions();
        assignPermissionsToRoles();
        initUsers();
    }

    private void initRoles() {
        Long count = em.createQuery("SELECT COUNT(r) FROM RoleJpaEntity r", Long.class).getSingleResult();
        if (count > 0) return;

        em.persist(RoleJpaEntity.fromDomain(Role.create("MEMBER", "Học viên (nội bộ/ngoài)")));
        em.persist(RoleJpaEntity.fromDomain(Role.create("INSTRUCTOR", "Giảng viên")));
        em.persist(RoleJpaEntity.fromDomain(Role.create("STAFF", "Nhân viên / Trợ lý quản lý nội dung")));
        em.persist(RoleJpaEntity.fromDomain(Role.create("ADMIN_USER", "Quản lý tài khoản")));
        em.persist(RoleJpaEntity.fromDomain(Role.create("SUPER_ADMIN", "Quản trị viên tối cao")));

        log.info("Seeded 5 roles: MEMBER, INSTRUCTOR, STAFF, ADMIN_USER, SUPER_ADMIN");
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
        em.persist(PermissionJpaEntity.fromDomain(Permission.create("CREATE_LESSON", "Tạo bài giảng trong chương học")));
        em.persist(PermissionJpaEntity.fromDomain(Permission.create("EDIT_LESSON", "Sửa / Xóa bài giảng")));
        em.persist(PermissionJpaEntity.fromDomain(Permission.create("VIEW_USER", "Xem thông tin người dùng")));
        em.persist(PermissionJpaEntity.fromDomain(Permission.create("EDIT_USER", "Chỉnh sửa người dùng")));
        em.persist(PermissionJpaEntity.fromDomain(Permission.create("DELETE_USER", "Xóa người dùng")));
        em.persist(PermissionJpaEntity.fromDomain(Permission.create("MANAGE_ROLE", "Quản lý phân quyền")));
        em.persist(PermissionJpaEntity.fromDomain(Permission.create("VIEW_REPORT", "Xem báo cáo thống kê")));
        em.persist(PermissionJpaEntity.fromDomain(Permission.create("PUBLISH_COURSE", "Duyệt và publish khóa học")));
        em.persist(PermissionJpaEntity.fromDomain(Permission.create("LOCK_COURSE_PRICE", "Khóa giá / sửa giá đã khóa")));
        em.persist(PermissionJpaEntity.fromDomain(Permission.create("MANAGE_VOUCHER", "Tạo / sửa / xóa / xem voucher")));
        em.persist(PermissionJpaEntity.fromDomain(Permission.create("USE_VOUCHER", "Áp dụng voucher khi mua khóa học")));

        log.info("Seeded 18 permissions");
    }

    private void assignPermissionsToRoles() {
        // Kiểm tra xem đã gán permission chưa
        Long count = em.createQuery("SELECT COUNT(rp) FROM RolePermissionJpaEntity rp", Long.class).getSingleResult();
        if (count > 0) return;

        // Lấy tất cả roles
        RoleJpaEntity memberRole = em.createQuery("SELECT r FROM RoleJpaEntity r WHERE r.name = 'MEMBER'", RoleJpaEntity.class)
                .getSingleResult();
        RoleJpaEntity instructorRole = em.createQuery("SELECT r FROM RoleJpaEntity r WHERE r.name = 'INSTRUCTOR'", RoleJpaEntity.class)
                .getSingleResult();
        RoleJpaEntity staffRole = em.createQuery("SELECT r FROM RoleJpaEntity r WHERE r.name = 'STAFF'", RoleJpaEntity.class)
                .getSingleResult();
        RoleJpaEntity adminUserRole = em.createQuery("SELECT r FROM RoleJpaEntity r WHERE r.name = 'ADMIN_USER'", RoleJpaEntity.class)
                .getSingleResult();
        RoleJpaEntity superAdminRole = em.createQuery("SELECT r FROM RoleJpaEntity r WHERE r.name = 'SUPER_ADMIN'", RoleJpaEntity.class)
                .getSingleResult();

        // Lấy tất cả permissions
        PermissionJpaEntity viewCourse = getPermission("VIEW_COURSE");
        PermissionJpaEntity enrollCourse = getPermission("ENROLL_COURSE");
        PermissionJpaEntity createCourse = getPermission("CREATE_COURSE");
        PermissionJpaEntity editCourse = getPermission("EDIT_COURSE");
        PermissionJpaEntity deleteCourse = getPermission("DELETE_COURSE");
        PermissionJpaEntity createSection = getPermission("CREATE_SECTION");
        PermissionJpaEntity editSection = getPermission("EDIT_SECTION");
        PermissionJpaEntity createLesson = getPermission("CREATE_LESSON");
        PermissionJpaEntity editLesson = getPermission("EDIT_LESSON");
        PermissionJpaEntity viewUser = getPermission("VIEW_USER");
        PermissionJpaEntity editUser = getPermission("EDIT_USER");
        PermissionJpaEntity deleteUser = getPermission("DELETE_USER");
        PermissionJpaEntity manageRole = getPermission("MANAGE_ROLE");
        PermissionJpaEntity viewReport = getPermission("VIEW_REPORT");
        PermissionJpaEntity publishCourse = getPermission("PUBLISH_COURSE");
        PermissionJpaEntity lockCoursePrice = getPermission("LOCK_COURSE_PRICE");
        PermissionJpaEntity manageVoucher = getPermission("MANAGE_VOUCHER");
        PermissionJpaEntity useVoucher = getPermission("USE_VOUCHER");

        // Gán permission theo ma trận phân quyền
        // MEMBER: VIEW_COURSE, USE_VOUCHER (chỉ MEMBER được áp dụng voucher khi mua khóa)
        assignPermission(memberRole, viewCourse);
        assignPermission(memberRole, useVoucher);
        
        // INSTRUCTOR: VIEW_COURSE, CREATE_COURSE, EDIT_COURSE, DELETE_COURSE, CREATE_SECTION, EDIT_SECTION, CREATE_LESSON, EDIT_LESSON, VIEW_REPORT
        assignPermission(instructorRole, viewCourse);
        assignPermission(instructorRole, createCourse);
        assignPermission(instructorRole, editCourse);
        assignPermission(instructorRole, deleteCourse);
        assignPermission(instructorRole, createSection);
        assignPermission(instructorRole, editSection);
        assignPermission(instructorRole, createLesson);
        assignPermission(instructorRole, editLesson);
        assignPermission(instructorRole, viewReport);
        
        // STAFF: VIEW_COURSE, CREATE_COURSE, EDIT_COURSE, DELETE_COURSE, CREATE_SECTION, EDIT_SECTION, CREATE_LESSON, EDIT_LESSON,
        //        PUBLISH_COURSE, LOCK_COURSE_PRICE, MANAGE_VOUCHER
        assignPermission(staffRole, viewCourse);
        assignPermission(staffRole, createCourse);
        assignPermission(staffRole, editCourse);
        assignPermission(staffRole, deleteCourse);
        assignPermission(staffRole, createSection);
        assignPermission(staffRole, editSection);
        assignPermission(staffRole, createLesson);
        assignPermission(staffRole, editLesson);
        assignPermission(staffRole, publishCourse);
        assignPermission(staffRole, lockCoursePrice);
        assignPermission(staffRole, manageVoucher);
        
        // ADMIN_USER: VIEW_COURSE, CREATE_COURSE, EDIT_COURSE, DELETE_COURSE, VIEW_USER, EDIT_USER
        assignPermission(adminUserRole, viewCourse);
        assignPermission(adminUserRole, createCourse);
        assignPermission(adminUserRole, editCourse);
        assignPermission(adminUserRole, deleteCourse);
        assignPermission(adminUserRole, viewUser);
        assignPermission(adminUserRole, editUser);
        
        // SUPER_ADMIN: Tất cả permissions
        assignPermission(superAdminRole, viewCourse);
        assignPermission(superAdminRole, enrollCourse);
        assignPermission(superAdminRole, createCourse);
        assignPermission(superAdminRole, editCourse);
        assignPermission(superAdminRole, deleteCourse);
        assignPermission(superAdminRole, createSection);
        assignPermission(superAdminRole, editSection);
        assignPermission(superAdminRole, createLesson);
        assignPermission(superAdminRole, editLesson);
        assignPermission(superAdminRole, viewUser);
        assignPermission(superAdminRole, editUser);
        assignPermission(superAdminRole, deleteUser);
        assignPermission(superAdminRole, manageRole);
        assignPermission(superAdminRole, viewReport);
        assignPermission(superAdminRole, publishCourse);
        assignPermission(superAdminRole, lockCoursePrice);
        assignPermission(superAdminRole, manageVoucher);
        assignPermission(superAdminRole, useVoucher);

        log.info("Assigned permissions to roles according to permission matrix");
    }

    private PermissionJpaEntity getPermission(String name) {
        return em.createQuery("SELECT p FROM PermissionJpaEntity p WHERE p.name = :name", PermissionJpaEntity.class)
                .setParameter("name", name)
                .getSingleResult();
    }

    private void assignPermission(RoleJpaEntity role, PermissionJpaEntity permission) {
        em.createNativeQuery(
                "INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)")
                .setParameter(1, role.getId())
                .setParameter(2, permission.getId())
                .executeUpdate();
    }

    private void initUsers() {
        Long count = em.createQuery("SELECT COUNT(u) FROM UserJpaEntity u", Long.class).getSingleResult();
        if (count > 0) return;

        RoleJpaEntity memberRole     = getRole("MEMBER");
        RoleJpaEntity instructorRole = getRole("INSTRUCTOR");
        RoleJpaEntity staffRole      = getRole("STAFF");
        RoleJpaEntity adminUserRole  = getRole("ADMIN_USER");
        RoleJpaEntity superAdminRole = getRole("SUPER_ADMIN");

        String pw = passwordEncoder.encode("password123");

        // SUPER_ADMIN
        em.persist(buildUser("superadmin", "superadmin@example.com", pw,
                "Super Admin", superAdminRole, false, new java.math.BigDecimal("10000000")));

        // ADMIN_USER
        em.persist(buildUser("adminuser", "admin@example.com", pw,
                "Admin User", adminUserRole, false, java.math.BigDecimal.ZERO));

        // STAFF
        em.persist(buildUser("staff01", "staff@example.com", pw,
                "Nhân Viên Staff", staffRole, false, java.math.BigDecimal.ZERO));

        // INSTRUCTOR
        em.persist(buildUser("instructor01", "instructor@example.com", pw,
                "Giảng Viên Nguyễn", instructorRole, false, java.math.BigDecimal.ZERO));

        // MEMBER (external) — có sẵn balance để test mua khóa học
        em.persist(buildUser("member01", "member@example.com", pw,
                "Học Viên Trần", memberRole, false, new java.math.BigDecimal("5000000")));

        // MEMBER (internal) — luôn mua 0đ
        em.persist(buildUser("member_internal", "member.internal@example.com", pw,
                "Học Viên Nội Bộ", memberRole, true, java.math.BigDecimal.ZERO));

        log.info("Seeded 6 users: superadmin / adminuser / staff01 / instructor01 / member01 / member_internal (password: password123)");
    }

    private RoleJpaEntity getRole(String name) {
        return em.createQuery("SELECT r FROM RoleJpaEntity r WHERE r.name = :name", RoleJpaEntity.class)
                .setParameter("name", name)
                .getSingleResult();
    }

    private UserJpaEntity buildUser(String username, String email, String encodedPassword,
                                    String name, RoleJpaEntity role,
                                    boolean isInternal, java.math.BigDecimal balance) {
        UserJpaEntity u = new UserJpaEntity();
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword(encodedPassword);
        u.setName(name);
        u.setRole(role);
        u.setInternal(isInternal);
        u.setBalance(balance);
        return u;
    }
}
