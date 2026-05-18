package com.example.learning_system_spring.application.usecase;

import com.example.learning_system_spring.application.dto.GetCourseDetailInput;
import com.example.learning_system_spring.application.dto.GetCourseDetailOutput;
import com.example.learning_system_spring.application.repository.CourseRepository;
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
    public GetCourseDetailOutput execute(GetCourseDetailInput input) {
        Course course = courseRepository.findById(input.id())
                .orElseThrow(() -> new CourseNotFoundException(input.id()));

        return GetCourseDetailOutput.from(course);
    }
}
