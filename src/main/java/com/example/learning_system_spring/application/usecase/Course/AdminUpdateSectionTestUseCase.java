package com.example.learning_system_spring.application.usecase.Course;

import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.repository.Course.CourseSectionRepository;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.CourseSection;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.service.SectionAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUpdateSectionTestUseCase {

    private final CourseRepository courseRepo;
    private final CourseSectionRepository sectionRepo;
    private final SectionAuthorizationService authService;

    @Transactional
    public void execute(Long sectionId, String testContent, Long requestUserId, Role requesterRole) {
        Long courseId = sectionRepo.findCourseIdBySectionId(sectionId);
        if (courseId == null) {
            throw new IllegalArgumentException("Course not found for section");
        }

        Course course = courseRepo.findById(courseId).orElseThrow();
        authService.authorizeEditOrDelete(course, requestUserId, requesterRole);

        sectionRepo.updateTestContent(sectionId, testContent);
    }
}
