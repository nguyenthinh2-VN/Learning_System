package com.example.learning_system_spring.application.usecase.Section;

import com.example.learning_system_spring.application.dto.Section.DeleteSectionInput;
import com.example.learning_system_spring.application.repository.Course.CourseSectionRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.exception.SectionNotFoundException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.service.SectionAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteSectionUseCase {

    private final CourseRepository courseRepository;
    private final CourseSectionRepository sectionRepository;
    private final SectionAuthorizationService authorizationService;

    @Transactional
    public void execute(DeleteSectionInput input) {
        // Kiểm tra section tồn tại
        sectionRepository.findById(input.sectionId())
                .orElseThrow(() -> new SectionNotFoundException(input.sectionId()));

        // Kiểm tra course tồn tại + lấy instructorId để authorize
        Course course = courseRepository.findById(input.courseId())
                .orElseThrow(() -> new CourseNotFoundException(input.courseId()));

        authorizationService.authorizeEditOrDelete(course, input.requesterId(), input.requesterRole());

        sectionRepository.deleteById(input.sectionId());
    }
}
