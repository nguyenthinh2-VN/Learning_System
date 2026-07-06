package com.example.learning_system_spring.application.usecase.Course;

import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.repository.Course.CourseSectionRepository;
import com.example.learning_system_spring.domain.exception.SectionNotFoundException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.CourseSection;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.service.SectionAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminGetSectionTestUseCase {

    private final CourseRepository courseRepo;
    private final CourseSectionRepository sectionRepo;
    private final SectionAuthorizationService authService;

    @Transactional(readOnly = true)
    public String execute(Long sectionId, Long requesterId, Role requesterRole) {
        Long courseId = sectionRepo.findCourseIdBySectionId(sectionId);
        if (courseId == null) {
            throw new SectionNotFoundException(sectionId);
        }

        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        authService.authorizeEditOrDelete(course, requesterId, requesterRole);

        CourseSection section = sectionRepo.findById(sectionId)
                .orElseThrow(() -> new SectionNotFoundException(sectionId));

        return section.getTestContent();
    }
}
