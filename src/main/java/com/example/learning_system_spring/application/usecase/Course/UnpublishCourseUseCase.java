package com.example.learning_system_spring.application.usecase.Course;

import com.example.learning_system_spring.application.dto.Course.CourseOutput;
import com.example.learning_system_spring.application.dto.Course.UnpublishCourseInput;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.domain.exception.CourseAccessDeniedException;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.service.CourseOwnershipPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UnpublishCourseUseCase {

    private final CourseRepository courseRepository;

    @Transactional
    public CourseOutput execute(UnpublishCourseInput input) {
        if (!CourseOwnershipPolicy.hasFullAccess(input.requesterRole())) {
            throw new CourseAccessDeniedException("Bạn không có quyền ẩn khóa học.");
        }

        Course course = courseRepository.findByIdForUpdate(input.courseId())
                .orElseThrow(() -> new CourseNotFoundException(input.courseId()));

        course.unpublish();

        Course saved = courseRepository.save(course);
        return CourseOutput.from(saved);
    }
}
