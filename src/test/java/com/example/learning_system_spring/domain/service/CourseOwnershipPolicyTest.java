package com.example.learning_system_spring.domain.service;

import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Adversarial test cho CourseOwnershipPolicy.
 * Mục tiêu: phơi bày NPE khi role null — Bug-C.
 */
class CourseOwnershipPolicyTest {

    private final Role member = Role.reconstitute(1L, "MEMBER", null);
    private final Role instructor = Role.reconstitute(2L, "INSTRUCTOR", null);
    private final Role staff = Role.reconstitute(3L, "STAFF", null);
    private final Role adminUser = Role.reconstitute(4L, "ADMIN_USER", null);
    private final Role superAdmin = Role.reconstitute(5L, "SUPER_ADMIN", null);

    private Course courseOwnedBy(Long instructorId) {
        return Course.create("X", "y", 100, BigDecimal.TEN, instructorId, null, List.of());
    }

    @Nested
    @DisplayName("isOwner")
    class IsOwner {

        @Test
        @DisplayName("instructorId == requesterId → true")
        void match() {
            Course c = courseOwnedBy(1L);
            assertThat(CourseOwnershipPolicy.isOwner(c, 1L)).isTrue();
        }

        @Test
        @DisplayName("requesterId khác → false")
        void mismatch() {
            Course c = courseOwnedBy(1L);
            assertThat(CourseOwnershipPolicy.isOwner(c, 2L)).isFalse();
        }

        @Test
        @DisplayName("instructorId null → false (không NPE)")
        void instructorIdNull() {
            Course c = courseOwnedBy(null);
            assertThat(CourseOwnershipPolicy.isOwner(c, 1L)).isFalse();
        }

        @Test
        @DisplayName("requesterId null + instructorId set → false")
        void requesterIdNull() {
            Course c = courseOwnedBy(1L);
            assertThat(CourseOwnershipPolicy.isOwner(c, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("hasFullAccess (STAFF / SUPER_ADMIN)")
    class HasFullAccess {

        @Test
        @DisplayName("MEMBER → false")
        void memberFalse() {
            assertThat(CourseOwnershipPolicy.hasFullAccess(member)).isFalse();
        }

        @Test
        @DisplayName("INSTRUCTOR → false")
        void instructorFalse() {
            assertThat(CourseOwnershipPolicy.hasFullAccess(instructor)).isFalse();
        }

        @Test
        @DisplayName("STAFF → true")
        void staffTrue() {
            assertThat(CourseOwnershipPolicy.hasFullAccess(staff)).isTrue();
        }

        @Test
        @DisplayName("ADMIN_USER → false (không có MANAGE_VOUCHER)")
        void adminUserFalse() {
            assertThat(CourseOwnershipPolicy.hasFullAccess(adminUser)).isFalse();
        }

        @Test
        @DisplayName("SUPER_ADMIN → true")
        void superAdminTrue() {
            assertThat(CourseOwnershipPolicy.hasFullAccess(superAdmin)).isTrue();
        }

        @Test
        @DisplayName("Bug-C: role null → NullPointerException (không an toàn)")
        void nullRoleNpe() {
            // BUG: hasFullAccess không null-check trên role.
            // Tất cả CRUD voucher use case đều gọi: hasFullAccess(input.requesterRole())
            // Nếu role null (token bị tampered, JWT lỗi parse) → NPE escape ra GlobalExceptionHandler
            // → trả 500 INTERNAL_ERROR thay vì 403 ACCESS_DENIED.
            assertThatThrownBy(() -> CourseOwnershipPolicy.hasFullAccess(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("hasFullCourseAccess (STAFF / ADMIN_USER / SUPER_ADMIN)")
    class HasFullCourseAccess {

        @Test
        @DisplayName("ADMIN_USER → true (course-level CRUD)")
        void adminUserTrue() {
            assertThat(CourseOwnershipPolicy.hasFullCourseAccess(adminUser)).isTrue();
        }

        @Test
        @DisplayName("INSTRUCTOR → false")
        void instructorFalse() {
            assertThat(CourseOwnershipPolicy.hasFullCourseAccess(instructor)).isFalse();
        }
    }

    @Nested
    @DisplayName("isInstructorOwner")
    class IsInstructorOwner {

        @Test
        @DisplayName("INSTRUCTOR + đúng owner → true")
        void instructorOwnerTrue() {
            Course c = courseOwnedBy(2L);
            assertThat(CourseOwnershipPolicy.isInstructorOwner(c, 2L, instructor)).isTrue();
        }

        @Test
        @DisplayName("INSTRUCTOR + sai owner → false")
        void instructorWrongOwner() {
            Course c = courseOwnedBy(2L);
            assertThat(CourseOwnershipPolicy.isInstructorOwner(c, 999L, instructor)).isFalse();
        }

        @Test
        @DisplayName("STAFF + đúng owner → false (chỉ INSTRUCTOR mới qua check này)")
        void staffOwnerFalse() {
            Course c = courseOwnedBy(2L);
            assertThat(CourseOwnershipPolicy.isInstructorOwner(c, 2L, staff)).isFalse();
        }
    }

    @Nested
    @DisplayName("canViewUnpublished")
    class CanViewUnpublished {

        @Test
        @DisplayName("Owner xem được course chưa publish")
        void ownerCanView() {
            Course c = courseOwnedBy(2L);
            assertThat(CourseOwnershipPolicy.canViewUnpublished(c, 2L, instructor)).isTrue();
        }

        @Test
        @DisplayName("STAFF xem được course chưa publish của ai cũng được")
        void staffCanView() {
            Course c = courseOwnedBy(2L);
            assertThat(CourseOwnershipPolicy.canViewUnpublished(c, 999L, staff)).isTrue();
        }

        @Test
        @DisplayName("MEMBER không xem được course chưa publish")
        void memberCannotView() {
            Course c = courseOwnedBy(2L);
            assertThat(CourseOwnershipPolicy.canViewUnpublished(c, 999L, member)).isFalse();
        }

        @Test
        @DisplayName("role null → false (đã có null guard ở method này)")
        void nullRoleSafe() {
            Course c = courseOwnedBy(2L);
            // Ở đây chỉ method này có null guard. Test xác nhận để contrast với Bug-C ở hasFullAccess.
            assertThat(CourseOwnershipPolicy.canViewUnpublished(c, 2L, null)).isFalse();
        }
    }
}
