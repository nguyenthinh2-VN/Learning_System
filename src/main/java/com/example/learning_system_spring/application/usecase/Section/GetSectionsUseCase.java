package com.example.learning_system_spring.application.usecase.Section;

import com.example.learning_system_spring.application.dto.Section.SectionOutput;
import com.example.learning_system_spring.application.repository.Course.CourseSectionRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetSectionsUseCase {

    private final CourseRepository courseRepository;
    private final CourseSectionRepository sectionRepository;

    @Transactional(readOnly = true)
    public List<SectionOutput> execute(Long courseId) {
        // Kiểm tra course tồn tại
        courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        return sectionRepository.findByCourseId(courseId).stream()
                .map(SectionOutput::from)
                .collect(Collectors.toList());
    }
}
