package com.example.learning_system_spring.application.usecase.Course;

import com.example.learning_system_spring.application.dto.Course.GetCourseDetailInput;
import com.example.learning_system_spring.application.dto.Course.CourseOutput;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.model.Course;
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

        return CourseOutput.from(course);
    }
}
