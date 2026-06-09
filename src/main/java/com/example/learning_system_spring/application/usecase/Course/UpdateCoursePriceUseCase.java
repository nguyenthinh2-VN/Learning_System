package com.example.learning_system_spring.application.usecase.Course;

import com.example.learning_system_spring.application.dto.Course.CourseOutput;
import com.example.learning_system_spring.application.dto.Course.UpdateCoursePriceInput;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.model.Course;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cập nhật giá course bởi admin (bypass priceLocked). Đây là endpoint riêng biệt
 * với UpdateCourseUseCase. Authz enforce ở controller: hasAuthority('LOCK_COURSE_PRICE').
 */
@Service
@RequiredArgsConstructor
public class UpdateCoursePriceUseCase {

    private final CourseRepository courseRepository;

    @Transactional
    public CourseOutput execute(UpdateCoursePriceInput input) {
        Course course = courseRepository.findByIdForUpdate(input.courseId())
                .orElseThrow(() -> new CourseNotFoundException(input.courseId()));

        course.updatePrice(input.newPrice(), true);

        Course saved = courseRepository.save(course);
        return CourseOutput.from(saved);
    }
}
