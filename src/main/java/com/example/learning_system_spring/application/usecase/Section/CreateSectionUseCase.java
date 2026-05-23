package com.example.learning_system_spring.application.usecase.Section;

import com.example.learning_system_spring.application.dto.Section.CreateSectionInput;
import com.example.learning_system_spring.application.dto.Section.SectionOutput;
import com.example.learning_system_spring.application.repository.Course.CourseSectionRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.CourseSection;
import com.example.learning_system_spring.domain.service.SectionAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CreateSectionUseCase {

    private final CourseRepository courseRepository;
    private final CourseSectionRepository sectionRepository;
    private final SectionAuthorizationService authorizationService;

    @Transactional
    public SectionOutput execute(CreateSectionInput input) {
        Course course = courseRepository.findById(input.courseId())
                .orElseThrow(() -> new CourseNotFoundException(input.courseId()));

        authorizationService.authorizeCreate(course, input.requesterId(), input.requesterRole());

        CourseSection section = CourseSection.create(input.title(), input.orderIndex(), new ArrayList<>());
        CourseSection saved = sectionRepository.save(section, input.courseId());

        return SectionOutput.from(saved);
    }
}
