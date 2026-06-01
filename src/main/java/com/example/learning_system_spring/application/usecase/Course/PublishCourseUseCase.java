package com.example.learning_system_spring.application.usecase.Course;

import com.example.learning_system_spring.application.dto.Course.CourseOutput;
import com.example.learning_system_spring.application.dto.Course.PublishCourseInput;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.model.Course;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublishCourseUseCase {

    private final CourseRepository courseRepository;

    @Transactional
    public CourseOutput execute(PublishCourseInput input) {
        // Authz: enforce ở controller bằng @PreAuthorize("hasAuthority('PUBLISH_COURSE')").
        Course course = courseRepository.findByIdForUpdate(input.courseId())
                .orElseThrow(() -> new CourseNotFoundException(input.courseId()));

        course.publish(input.requesterId());

        Course saved = courseRepository.save(course);
        return CourseOutput.from(saved);
    }
}
