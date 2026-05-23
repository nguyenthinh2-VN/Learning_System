package com.example.learning_system_spring.application.usecase.Section;

import com.example.learning_system_spring.application.dto.Section.SectionOutput;
import com.example.learning_system_spring.application.dto.Section.UpdateSectionInput;
import com.example.learning_system_spring.application.repository.Course.CourseSectionRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.exception.SectionNotFoundException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.CourseSection;
import com.example.learning_system_spring.domain.service.SectionAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateSectionUseCase {

    private final CourseRepository courseRepository;
    private final CourseSectionRepository sectionRepository;
    private final SectionAuthorizationService authorizationService;

    @Transactional
    public SectionOutput execute(UpdateSectionInput input) {
        // Kiểm tra section tồn tại
        CourseSection existing = sectionRepository.findById(input.sectionId())
                .orElseThrow(() -> new SectionNotFoundException(input.sectionId()));

        // Kiểm tra course tồn tại + lấy instructorId để authorize
        Course course = courseRepository.findById(input.courseId())
                .orElseThrow(() -> new CourseNotFoundException(input.courseId()));

        authorizationService.authorizeEditOrDelete(course, input.requesterId(), input.requesterRole());

        // Tái tạo section với dữ liệu mới, giữ nguyên lessons cũ
        CourseSection updated = CourseSection.reconstitute(
                existing.getId(),
                input.title(),
                input.orderIndex(),
                existing.getLessons());

        CourseSection saved = sectionRepository.save(updated, input.courseId());
        return SectionOutput.from(saved);
    }
}
