package com.example.learning_system_spring.domain.service;

import com.example.learning_system_spring.domain.exception.LessonAccessDeniedException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test cho LessonAuthorizationService.authorizeView().
 * Kiểm tra toàn bộ ma trận phân quyền xem lesson.
 */
@DisplayName("LessonAuthorizationService.authorizeView()")
class LessonAuthorizationServiceTest {

    // ── Helpers ───────────────────────────────────────────────────
    private static final Long OWNER_ID = 10L;
    private static final Long OTHER_ID = 99L;

    private Course courseOwnedBy(Long instructorId) {
        return Course.reconstitute(
                1L, "Test Course", "desc", 100, 0,
                new BigDecimal("500000"), instructorId,
                null, false, false, null, null,
                List.of());
    }

    private Role role(String name) {
        return Role.reconstitute(1L, name, null);
    }

    // ─────────────────────────────────────────────────────────────
    // SUPER_ADMIN
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("SUPER_ADMIN")
    class SuperAdmin {

        @Test
        @DisplayName("luôn được phép — enrolled=false")
        void alwaysAllowed() {
            assertThatCode(() ->
                    LessonAuthorizationService.authorizeView(
                            courseOwnedBy(OWNER_ID), OTHER_ID, role("SUPER_ADMIN"), false))
                    .doesNotThrowAnyException();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // STAFF
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("STAFF")
    class Staff {

        @Test
        @DisplayName("luôn được phép — enrolled=false")
        void alwaysAllowed() {
            assertThatCode(() ->
                    LessonAuthorizationService.authorizeView(
                            courseOwnedBy(OWNER_ID), OTHER_ID, role("STAFF"), false))
                    .doesNotThrowAnyException();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // INSTRUCTOR
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("INSTRUCTOR")
    class Instructor {

        @Test
        @DisplayName("chủ sở hữu → được phép")
        void ownerAllowed() {
            assertThatCode(() ->
                    LessonAuthorizationService.authorizeView(
                            courseOwnedBy(OWNER_ID), OWNER_ID, role("INSTRUCTOR"), false))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("không phải chủ sở hữu → 403")
        void nonOwnerDenied() {
            assertThatThrownBy(() ->
                    LessonAuthorizationService.authorizeView(
                            courseOwnedBy(OWNER_ID), OTHER_ID, role("INSTRUCTOR"), false))
                    .isInstanceOf(LessonAccessDeniedException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // MEMBER
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("MEMBER")
    class Member {

        @Test
        @DisplayName("đã enrolled → được phép")
        void enrolledAllowed() {
            assertThatCode(() ->
                    LessonAuthorizationService.authorizeView(
                            courseOwnedBy(OWNER_ID), OTHER_ID, role("MEMBER"), true))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("chưa enrolled → 403")
        void notEnrolledDenied() {
            assertThatThrownBy(() ->
                    LessonAuthorizationService.authorizeView(
                            courseOwnedBy(OWNER_ID), OTHER_ID, role("MEMBER"), false))
                    .isInstanceOf(LessonAccessDeniedException.class)
                    .hasMessageContaining("chưa đăng ký");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ADMIN_USER
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("ADMIN_USER")
    class AdminUser {

        @Test
        @DisplayName("luôn bị từ chối — không thuộc luồng học tập")
        void alwaysDenied() {
            assertThatThrownBy(() ->
                    LessonAuthorizationService.authorizeView(
                            courseOwnedBy(OWNER_ID), OTHER_ID, role("ADMIN_USER"), false))
                    .isInstanceOf(LessonAccessDeniedException.class);
        }

        @Test
        @DisplayName("ADMIN_USER với enrolled=true vẫn bị từ chối")
        void deniedEvenIfEnrolled() {
            assertThatThrownBy(() ->
                    LessonAuthorizationService.authorizeView(
                            courseOwnedBy(OWNER_ID), OTHER_ID, role("ADMIN_USER"), true))
                    .isInstanceOf(LessonAccessDeniedException.class);
        }
    }
}
