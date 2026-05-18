package com.example.learning_system_spring.application.usecase.course;

import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.repository.Course.EnrollmentRepository;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.application.usecase.Course.PurchaseCourseUseCase;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.exception.UserNotFoundException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.Enrollment;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.model.User;
import com.example.learning_system_spring.infrastructure.service.PurchaseLedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PurchaseCourseUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private PurchaseLedgerService purchaseLedgerService;

    @InjectMocks
    private PurchaseCourseUseCase purchaseCourseUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_ShouldPurchaseSuccessfully_WhenBalanceIsSufficient() {
        // Arrange
        Role memberRole = Role.reconstitute(1L, "MEMBER", "Học viên");
        User user = User.reconstitute(1L, "MEM123", "user@test.com", "pass", "User", memberRole, false,
                new BigDecimal("1000.00"), null, null);
        Course course = Course.reconstitute(1L, "Course 1", "Desc", 100, 0, new BigDecimal("500.00"), 100L, null);

        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserIdAndCourseId(1L, 1L)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> {
            Enrollment e = i.getArgument(0);
            return Enrollment.reconstitute(1L, e.getUserId(), e.getCourseId(), e.getPaidPrice(), e.getEnrolledAt());
        });

        // Act
        Enrollment enrollment = purchaseCourseUseCase.execute(1L, 1L);

        // Assert
        assertNotNull(enrollment);
        assertEquals(new BigDecimal("500.00"), enrollment.getPaidPrice());
        assertEquals(new BigDecimal("500.00"), user.getBalance());
        assertEquals(1, course.getEnrolledCount());

        verify(userRepository).save(user);
        verify(courseRepository).save(course);
        verify(purchaseLedgerService).logPurchase(eq(1L), eq(1L), eq(new BigDecimal("500.00")), any());
    }

    @Test
    void execute_ShouldThrowException_WhenBalanceInsufficient() {
        // Arrange
        Role memberRole = Role.reconstitute(1L, "MEMBER", "Học viên");
        User user = User.reconstitute(1L, "MEM123", "user@test.com", "pass", "User", memberRole, false,
                new BigDecimal("100.00"), null, null);
        Course course = Course.reconstitute(1L, "Course 1", "Desc", 100, 0, new BigDecimal("500.00"), 100L, null);

        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserIdAndCourseId(1L, 1L)).thenReturn(false);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> purchaseCourseUseCase.execute(1L, 1L));
        assertEquals("Số dư không đủ để thanh toán khóa học. Vui lòng nạp thêm tiền.", exception.getMessage());

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void execute_ShouldThrowException_WhenAlreadyEnrolled() {
        // Arrange
        Role memberRole = Role.reconstitute(1L, "MEMBER", "Học viên");
        User user = User.reconstitute(1L, "MEM123", "user@test.com", "pass", "User", memberRole, false,
                new BigDecimal("1000.00"), null, null);
        Course course = Course.reconstitute(1L, "Course 1", "Desc", 100, 0, new BigDecimal("500.00"), 100L, null);

        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserIdAndCourseId(1L, 1L)).thenReturn(true);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> purchaseCourseUseCase.execute(1L, 1L));
        assertEquals("Bạn đã đăng ký khóa học này rồi.", exception.getMessage());
    }
}
