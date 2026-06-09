package com.example.learning_system_spring.application.usecase.Cart;

import com.example.learning_system_spring.application.dto.Cart.AddCourseToCartInput;
import com.example.learning_system_spring.application.repository.Cart.CartItemRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.repository.Course.EnrollmentRepository;
import com.example.learning_system_spring.domain.exception.AlreadyEnrolledException;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.exception.CourseNotPublishedException;
import com.example.learning_system_spring.domain.model.CartItem;
import com.example.learning_system_spring.domain.model.Course;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddCourseToCartUseCase {

    private final CartItemRepository cartItemRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public void execute(AddCourseToCartInput input) {
        Long userId = input.requesterId();
        
        for (Long courseId : input.courseIds()) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new CourseNotFoundException(courseId));

            if (!course.isPublished()) {
                throw new CourseNotPublishedException(courseId);
            }

            if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
                throw new AlreadyEnrolledException(userId, courseId);
            }

            if (!cartItemRepository.existsByUserIdAndCourseId(userId, courseId)) {
                cartItemRepository.save(CartItem.create(userId, courseId));
            }
        }
    }
}
