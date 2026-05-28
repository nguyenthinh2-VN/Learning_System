package com.example.learning_system_spring.domain.model;

import com.example.learning_system_spring.domain.exception.CourseAlreadyPublishedException;
import com.example.learning_system_spring.domain.exception.CoursePriceLockedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Adversarial test cho Course domain — tập trung vào Course Approval Workflow:
 * publish, unpublish, updatePrice với priceLocked.
 */
class CourseTest {

    private Course freshCourse(BigDecimal price) {
        return Course.create("Test Course", "desc", 100, price, 1L, null, List.of());
    }

    @Nested
    @DisplayName("Course.create defaults")
    class CreateDefaults {

        @Test
        @DisplayName("Course mới mặc định published = false, priceLocked = false, publishedAt/By = null")
        void defaultsAfterCreate() {
            Course c = freshCourse(new BigDecimal("100000"));
            assertThat(c.isPublished()).isFalse();
            assertThat(c.isPriceLocked()).isFalse();
            assertThat(c.getPublishedAt()).isNull();
            assertThat(c.getPublishedBy()).isNull();
        }

        @Test
        @DisplayName("price = null → mặc định BigDecimal.ZERO")
        void nullPriceBecomesZero() {
            Course c = freshCourse(null);
            assertThat(c.getPrice()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("maxStudents <= 0 → reject")
        void rejectZeroMaxStudents() {
            assertThatThrownBy(() -> Course.create("X", "y", 0, BigDecimal.TEN, 1L, null, List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("publish workflow")
    class Publish {

        @Test
        @DisplayName("publish(adminId) → published = true, priceLocked = true, publishedAt set, publishedBy = adminId")
        void publishHappyPath() {
            Course c = freshCourse(new BigDecimal("500000"));
            c.publish(99L);
            assertThat(c.isPublished()).isTrue();
            assertThat(c.isPriceLocked()).isTrue();
            assertThat(c.getPublishedAt()).isNotNull();
            assertThat(c.getPublishedBy()).isEqualTo(99L);
        }

        @Test
        @DisplayName("publish lần 2 → CourseAlreadyPublishedException")
        void publishTwice() {
            Course c = freshCourse(new BigDecimal("500000"));
            c.publish(1L);
            assertThatThrownBy(() -> c.publish(1L))
                    .isInstanceOf(CourseAlreadyPublishedException.class);
        }

        @Test
        @DisplayName("price = 0 → vẫn publish được (course miễn phí)")
        void publishFreeCourseAllowed() {
            Course c = freshCourse(BigDecimal.ZERO);
            c.publish(1L);
            assertThat(c.isPublished()).isTrue();
        }

        @Test
        @DisplayName("Bug observation: course price = null sau reconstitute → publish chấp nhận (do create set ZERO mặc định)")
        void publishWithNullPriceFromReconstitute() {
            // Course.reconstitute với price = null cũng được set ZERO ở Course.builder.
            // Test xác nhận behavior này — course "không có giá" coi như miễn phí.
            Course c = Course.reconstitute(1L, "X", "y", 100, 0,
                    null, 1L, null, false, false, null, null, List.of());
            assertThat(c.getPrice()).isEqualByComparingTo("0");
            c.publish(1L);
            assertThat(c.isPublished()).isTrue();
        }
    }

    @Nested
    @DisplayName("unpublish")
    class Unpublish {

        @Test
        @DisplayName("unpublish course đã publish → published = false, priceLocked GIỮ NGUYÊN = true")
        void unpublishKeepsPriceLocked() {
            Course c = freshCourse(new BigDecimal("100000"));
            c.publish(1L);
            c.unpublish();
            assertThat(c.isPublished()).isFalse();
            assertThat(c.isPriceLocked()).isTrue();
        }

        @Test
        @DisplayName("Bug observation: unpublish course chưa publish → KHÔNG ném exception (idempotent)")
        void unpublishUnpublishedSilent() {
            // Code: unpublish() chỉ set published = false, không kiểm tra trạng thái hiện tại.
            // Nếu spec yêu cầu "chỉ unpublish được khi đang published" thì đây là bug.
            // Hiện tại impl chấp nhận → idempotent (không nguy hiểm nhưng ít chuẩn).
            Course c = freshCourse(new BigDecimal("100000"));
            c.unpublish();  // không exception
            assertThat(c.isPublished()).isFalse();
        }
    }

    @Nested
    @DisplayName("updatePrice")
    class UpdatePrice {

        @Test
        @DisplayName("priceLocked = false + isAdmin = false → INSTRUCTOR sửa được")
        void instructorSetPriceWhenUnlocked() {
            Course c = freshCourse(new BigDecimal("100000"));
            c.updatePrice(new BigDecimal("200000"), false);
            assertThat(c.getPrice()).isEqualByComparingTo("200000");
        }

        @Test
        @DisplayName("priceLocked = true + isAdmin = false → CoursePriceLockedException")
        void instructorBlockedWhenLocked() {
            Course c = freshCourse(new BigDecimal("100000"));
            c.publish(1L);  // priceLocked → true
            assertThatThrownBy(() -> c.updatePrice(new BigDecimal("200000"), false))
                    .isInstanceOf(CoursePriceLockedException.class);
        }

        @Test
        @DisplayName("priceLocked = true + isAdmin = true → Admin override OK")
        void adminOverrideWhenLocked() {
            Course c = freshCourse(new BigDecimal("100000"));
            c.publish(1L);
            c.updatePrice(new BigDecimal("300000"), true);
            assertThat(c.getPrice()).isEqualByComparingTo("300000");
        }

        @Test
        @DisplayName("price âm → IllegalArgumentException (kể cả admin)")
        void rejectNegativePrice() {
            Course c = freshCourse(new BigDecimal("100000"));
            assertThatThrownBy(() -> c.updatePrice(new BigDecimal("-1"), true))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("price = null → IllegalArgumentException")
        void rejectNullPrice() {
            Course c = freshCourse(new BigDecimal("100000"));
            assertThatThrownBy(() -> c.updatePrice(null, true))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("price = 0 + isAdmin = true → cho phép (course thành miễn phí)")
        void zeroPriceByAdmin() {
            Course c = freshCourse(new BigDecimal("100000"));
            c.publish(1L);
            c.updatePrice(BigDecimal.ZERO, true);
            assertThat(c.getPrice()).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("enroll")
    class Enroll {

        @Test
        @DisplayName("enroll trong khi đang full → IllegalStateException")
        void enrollWhenFull() {
            Course c = Course.create("X", "y", 1, BigDecimal.TEN, 1L, null, List.of());
            c.enroll();
            assertThatThrownBy(c::enroll).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Multiple enrolls tăng enrolledCount đúng")
        void multipleEnrolls() {
            Course c = Course.create("X", "y", 5, BigDecimal.TEN, 1L, null, List.of());
            for (int i = 0; i < 5; i++) c.enroll();
            assertThat(c.getEnrolledCount()).isEqualTo(5);
            assertThat(c.isFull()).isTrue();
        }
    }
}
