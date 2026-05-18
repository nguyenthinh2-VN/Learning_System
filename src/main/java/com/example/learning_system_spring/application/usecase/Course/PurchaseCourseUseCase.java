package com.example.learning_system_spring.application.usecase.Course;

import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.repository.Course.EnrollmentRepository;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.domain.exception.CourseAccessDeniedException;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.exception.UserNotFoundException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.Enrollment;
import com.example.learning_system_spring.domain.model.User;
import com.example.learning_system_spring.infrastructure.service.PurchaseLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PurchaseCourseUseCase {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PurchaseLedgerService purchaseLedgerService;

    @Transactional
    public Enrollment execute(Long userId, Long courseId) {
        // Use Pessimistic lock for both user and course to prevent double spending and over-enrollment
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Course course = courseRepository.findByIdForUpdate(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        if (course.isFull()) {
            throw new IllegalStateException("Khóa học đã đầy, không thể đăng ký thêm.");
        }

        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new IllegalStateException("Bạn đã đăng ký khóa học này rồi.");
        }

        // Determine price
        BigDecimal paidPrice = user.isInternal() ? BigDecimal.ZERO : course.getPrice();

        // Deduct balance
        if (paidPrice.compareTo(BigDecimal.ZERO) > 0) {
            try {
                user.deductBalance(paidPrice);
            } catch (IllegalStateException e) {
                throw new IllegalStateException("Số dư không đủ để thanh toán khóa học. Vui lòng nạp thêm tiền.");
            }
        }

        // Enroll
        course.enroll();

        // Save
        userRepository.save(user);
        courseRepository.save(course);

        Enrollment enrollment = Enrollment.create(userId, courseId, paidPrice);
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

        // Write to audit log
        purchaseLedgerService.logPurchase(userId, courseId, paidPrice, savedEnrollment.getEnrolledAt());

        return savedEnrollment;
    }
}
