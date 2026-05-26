package com.example.learning_system_spring.application.usecase.User;

import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.application.dto.User.MyEnrollmentOutput;
import com.example.learning_system_spring.application.repository.Course.EnrollmentRepository;
import com.example.learning_system_spring.domain.model.Enrollment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DisplayName("GetMyEnrollmentsUseCase")
class GetMyEnrollmentsUseCaseTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private GetMyEnrollmentsUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Enrollment makeEnrollment(Long id, Long courseId, BigDecimal price, LocalDateTime enrolledAt) {
        return Enrollment.reconstitute(id, 1L, courseId, price, enrolledAt);
    }

    // ─────────────────────────────────────────────────────────────
    // Validation
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("page = -1 → IllegalArgumentException")
        void negativePageRejected() {
            assertThatThrownBy(() -> useCase.execute(1L, -1, 20))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("page");
        }

        @Test
        @DisplayName("size = 0 → IllegalArgumentException")
        void zeroSizeRejected() {
            assertThatThrownBy(() -> useCase.execute(1L, 0, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("size");
        }

        @Test
        @DisplayName("size = 101 → IllegalArgumentException")
        void oversizedRejected() {
            assertThatThrownBy(() -> useCase.execute(1L, 0, 101))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("size");
        }

        @Test
        @DisplayName("page = 0, size = 1 → hợp lệ (boundary)")
        void minValidParams() {
            when(enrollmentRepository.findByUserId(1L, 0, 1))
                    .thenReturn(PageResult.of(0L, 0, 0, 1, List.of()));
            assertThat(useCase.execute(1L, 0, 1)).isNotNull();
        }

        @Test
        @DisplayName("page = 0, size = 100 → hợp lệ (boundary)")
        void maxValidSize() {
            when(enrollmentRepository.findByUserId(1L, 0, 100))
                    .thenReturn(PageResult.of(0L, 0, 0, 100, List.of()));
            assertThat(useCase.execute(1L, 0, 100)).isNotNull();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Kết quả rỗng
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("user chưa có enrollment → trả trang rỗng (không phải 403)")
    void emptyResult() {
        when(enrollmentRepository.findByUserId(1L, 0, 20))
                .thenReturn(PageResult.of(0L, 0, 0, 20, List.of()));

        PageResult<MyEnrollmentOutput> result = useCase.execute(1L, 0, 20);

        assertThat(result.items()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
    }

    // ─────────────────────────────────────────────────────────────
    // Kết quả một trang
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("user có 2 enrollment → trả đúng 2 item với đúng field")
    void singlePage() {
        LocalDateTime t1 = LocalDateTime.of(2026, 5, 20, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 5, 21, 10, 0);

        List<Enrollment> enrollments = List.of(
                makeEnrollment(1L, 10L, new BigDecimal("500000"), t1),
                makeEnrollment(2L, 20L, new BigDecimal("0"), t2)
        );
        when(enrollmentRepository.findByUserId(1L, 0, 20))
                .thenReturn(PageResult.of(2L, 1, 0, 20, enrollments));

        PageResult<MyEnrollmentOutput> result = useCase.execute(1L, 0, 20);

        assertThat(result.totalElements()).isEqualTo(2L);
        assertThat(result.items()).hasSize(2);

        MyEnrollmentOutput first = result.items().get(0);
        assertThat(first.enrollmentId()).isEqualTo(1L);
        assertThat(first.courseId()).isEqualTo(10L);
        assertThat(first.paidPrice()).isEqualByComparingTo("500000");
        assertThat(first.enrolledAt()).isEqualTo(t1);

        MyEnrollmentOutput second = result.items().get(1);
        assertThat(second.paidPrice()).isEqualByComparingTo("0");
    }

    // ─────────────────────────────────────────────────────────────
    // Phân trang
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("nhiều trang — metadata phân trang đúng")
    void multiPage() {
        List<Enrollment> page0Items = List.of(
                makeEnrollment(3L, 30L, BigDecimal.TEN, LocalDateTime.now()),
                makeEnrollment(4L, 40L, BigDecimal.TEN, LocalDateTime.now())
        );
        when(enrollmentRepository.findByUserId(1L, 0, 2))
                .thenReturn(PageResult.of(5L, 3, 0, 2, page0Items));

        PageResult<MyEnrollmentOutput> result = useCase.execute(1L, 0, 2);

        assertThat(result.totalElements()).isEqualTo(5L);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.items()).hasSize(2);
    }

    @Test
    @DisplayName("trang 2 — trả đúng page index")
    void page2() {
        List<Enrollment> page1Items = List.of(
                makeEnrollment(5L, 50L, BigDecimal.TEN, LocalDateTime.now())
        );
        when(enrollmentRepository.findByUserId(1L, 1, 2))
                .thenReturn(PageResult.of(5L, 3, 1, 2, page1Items));

        PageResult<MyEnrollmentOutput> result = useCase.execute(1L, 1, 2);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.items()).hasSize(1);
    }
}
