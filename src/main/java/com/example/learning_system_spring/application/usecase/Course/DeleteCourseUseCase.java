package com.example.learning_system_spring.application.usecase.Course;

import com.example.learning_system_spring.application.dto.Course.DeleteCourseInput;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.service.CourseAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteCourseUseCase {

    private final CourseRepository courseRepository;
    private final CourseAuthorizationService authorizationService;

    @Transactional
    public void execute(DeleteCourseInput input) {
        Course course = courseRepository.findById(input.courseId())
                .orElseThrow(() -> new CourseNotFoundException(input.courseId()));

        authorizationService.authorizeEditOrDelete(course, input.requesterId(), input.requesterRole());

        courseRepository.deleteById(input.courseId());
    }
}
