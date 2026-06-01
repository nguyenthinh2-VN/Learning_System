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
        assignPermissionsToRoles();
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

    /**
     * Seed permissions theo ma trận phân quyền (docs/permission-matrix.md).
     * Idempotent theo từng permission: chỉ insert permission nào CHƯA tồn tại,
     * nhờ vậy DB đã seed trước đó vẫn nhận được các permission mới khi khởi động lại.
     */
    private void initPermissions() {
        // 18 permission gốc
        seedPermission("VIEW_COURSE", "Xem khóa học");
        seedPermission("ENROLL_COURSE", "Đăng ký khóa học");
        seedPermission("CREATE_COURSE", "Tạo khóa học mới");
        seedPermission("EDIT_COURSE", "Chỉnh sửa khóa học");
        seedPermission("DELETE_COURSE", "Xóa khóa học");
        seedPermission("CREATE_SECTION", "Tạo chương học trong khóa học");
        seedPermission("EDIT_SECTION", "Sửa / Xóa chương học");
        seedPermission("CREATE_LESSON", "Tạo bài giảng trong chương học");
        seedPermission("EDIT_LESSON", "Sửa / Xóa bài giảng");
        seedPermission("VIEW_USER", "Xem thông tin người dùng");
        seedPermission("EDIT_USER", "Chỉnh sửa người dùng");
        seedPermission("DELETE_USER", "Xóa người dùng");
        seedPermission("MANAGE_ROLE", "Quản lý phân quyền");
        seedPermission("VIEW_REPORT", "Xem báo cáo thống kê");
        seedPermission("PUBLISH_COURSE", "Duyệt và publish khóa học");
        seedPermission("LOCK_COURSE_PRICE", "Khóa giá / sửa giá đã khóa");
        seedPermission("MANAGE_VOUCHER", "Tạo / sửa / xóa / xem voucher");
        seedPermission("USE_VOUCHER", "Áp dụng voucher khi mua khóa học");

        // Permission bổ sung (matrix #11, #20–#24) — chuẩn bị cho phân quyền động
        seedPermission("CREATE_USER", "Cấp tài khoản mới (nội bộ/ngoài)");
        seedPermission("VIEW_LESSON", "Xem nội dung bài giảng đã trả phí");
        seedPermission("TRACK_PROGRESS", "Đánh dấu hoàn thành & xem tiến độ học");
        seedPermission("VIEW_TRANSACTION", "Xem giao dịch toàn hệ thống");
        seedPermission("VIEW_REVENUE", "Xem báo cáo doanh thu");
        seedPermission("MANAGE_WALLET", "Admin cộng tiền thủ công vào ví user");
    }

    private void assignPermissionsToRoles() {
        // Lấy tất cả roles
        RoleJpaEntity memberRole = getRole("MEMBER");
        RoleJpaEntity instructorRole = getRole("INSTRUCTOR");
        RoleJpaEntity staffRole = getRole("STAFF");
        RoleJpaEntity adminUserRole = getRole("ADMIN_USER");
        RoleJpaEntity superAdminRole = getRole("SUPER_ADMIN");

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
        PermissionJpaEntity createUser = getPermission("CREATE_USER");
        PermissionJpaEntity viewLesson = getPermission("VIEW_LESSON");
        PermissionJpaEntity trackProgress = getPermission("TRACK_PROGRESS");
        PermissionJpaEntity viewTransaction = getPermission("VIEW_TRANSACTION");
        PermissionJpaEntity viewRevenue = getPermission("VIEW_REVENUE");
        PermissionJpaEntity manageWallet = getPermission("MANAGE_WALLET");

        // Gán permission theo ma trận phân quyền
        // MEMBER: VIEW_COURSE, USE_VOUCHER, VIEW_LESSON, TRACK_PROGRESS
        assignPermission(memberRole, viewCourse);
        assignPermission(memberRole, useVoucher);
        assignPermission(memberRole, viewLesson);
        assignPermission(memberRole, trackProgress);

        // INSTRUCTOR: VIEW_COURSE, CREATE_COURSE, EDIT_COURSE, DELETE_COURSE, CREATE_SECTION, EDIT_SECTION,
        //             CREATE_LESSON, EDIT_LESSON, VIEW_REPORT, VIEW_LESSON, TRACK_PROGRESS
        assignPermission(instructorRole, viewCourse);
        assignPermission(instructorRole, createCourse);
        assignPermission(instructorRole, editCourse);
        assignPermission(instructorRole, deleteCourse);
        assignPermission(instructorRole, createSection);
        assignPermission(instructorRole, editSection);
        assignPermission(instructorRole, createLesson);
        assignPermission(instructorRole, editLesson);
        assignPermission(instructorRole, viewReport);
        assignPermission(instructorRole, viewLesson);
        assignPermission(instructorRole, trackProgress);

        // STAFF: VIEW_COURSE, CREATE_COURSE, EDIT_COURSE, DELETE_COURSE, CREATE_SECTION, EDIT_SECTION,
        //        CREATE_LESSON, EDIT_LESSON, PUBLISH_COURSE, LOCK_COURSE_PRICE, MANAGE_VOUCHER,
        //        VIEW_LESSON, TRACK_PROGRESS
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
        assignPermission(staffRole, viewLesson);
        assignPermission(staffRole, trackProgress);

        // ADMIN_USER: VIEW_COURSE, CREATE_COURSE, EDIT_COURSE, DELETE_COURSE, VIEW_USER, EDIT_USER, CREATE_USER
        assignPermission(adminUserRole, viewCourse);
        assignPermission(adminUserRole, createCourse);
        assignPermission(adminUserRole, editCourse);
        assignPermission(adminUserRole, deleteCourse);
        assignPermission(adminUserRole, viewUser);
        assignPermission(adminUserRole, editUser);
        assignPermission(adminUserRole, createUser);

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
        assignPermission(superAdminRole, createUser);
        assignPermission(superAdminRole, viewLesson);
        assignPermission(superAdminRole, trackProgress);
        assignPermission(superAdminRole, viewTransaction);
        assignPermission(superAdminRole, viewRevenue);
        assignPermission(superAdminRole, manageWallet);

        log.info("Assigned permissions to roles according to permission matrix");
    }

    /** Insert permission nếu chưa tồn tại (idempotent theo name). */
    private void seedPermission(String name, String description) {
        Long count = em.createQuery(
                        "SELECT COUNT(p) FROM PermissionJpaEntity p WHERE p.name = :name", Long.class)
                .setParameter("name", name)
                .getSingleResult();
        if (count == 0) {
            em.persist(PermissionJpaEntity.fromDomain(Permission.create(name, description)));
            log.info("Seeded permission: {}", name);
        }
    }

    private RoleJpaEntity getRole(String name) {
        return em.createQuery("SELECT r FROM RoleJpaEntity r WHERE r.name = :name", RoleJpaEntity.class)
                .setParameter("name", name)
                .getSingleResult();
    }

    private PermissionJpaEntity getPermission(String name) {
        return em.createQuery("SELECT p FROM PermissionJpaEntity p WHERE p.name = :name", PermissionJpaEntity.class)
                .setParameter("name", name)
                .getSingleResult();
    }

    /**
     * Gán permission cho role nếu cặp (role, permission) chưa tồn tại (idempotent).
     * Dùng native query vì constructor entity là protected.
     */
    private void assignPermission(RoleJpaEntity role, PermissionJpaEntity permission) {
        Number existing = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM role_permissions WHERE role_id = ? AND permission_id = ?")
                .setParameter(1, role.getId())
                .setParameter(2, permission.getId())
                .getSingleResult();

        if (existing.longValue() == 0) {
            em.createNativeQuery(
                            "INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)")
                    .setParameter(1, role.getId())
                    .setParameter(2, permission.getId())
                    .executeUpdate();
        }
    }
}
