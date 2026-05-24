package com.example.learning_system_spring.application.usecase.Course;

import com.example.learning_system_spring.application.dto.Course.GetCourseDetailInput;
import com.example.learning_system_spring.application.dto.Course.CourseOutput;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.service.CourseOwnershipPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCourseDetailUseCase {
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public CourseOutput execute(GetCourseDetailInput input) {
        Course course = courseRepository.findById(input.id())
                .orElseThrow(() -> new CourseNotFoundException(input.id()));

        // Course chưa publish — chỉ owner / admin mới được xem detail.
        // Trả 404 (thay vì 403) để không tiết lộ sự tồn tại của course pending.
        if (!course.isPublished()) {
            boolean canView = input.requesterRole() != null
                    && CourseOwnershipPolicy.canViewUnpublished(course, input.requesterId(), input.requesterRole());
            if (!canView) {
                throw new CourseNotFoundException(input.id());
            }
        }

        return CourseOutput.from(course);
    }
}
