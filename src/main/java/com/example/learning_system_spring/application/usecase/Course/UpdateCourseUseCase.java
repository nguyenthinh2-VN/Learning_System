package com.example.learning_system_spring.application.usecase.Course;

import com.example.learning_system_spring.application.dto.Course.CourseLessonDto;
import com.example.learning_system_spring.application.dto.Course.CourseOutput;
import com.example.learning_system_spring.application.dto.Course.CourseSectionDto;
import com.example.learning_system_spring.application.dto.Course.UpdateCourseInput;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.CourseLesson;
import com.example.learning_system_spring.domain.model.CourseSection;
import com.example.learning_system_spring.domain.service.CourseAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UpdateCourseUseCase {

    private final CourseRepository courseRepository;
    private final CourseAuthorizationService authorizationService;

    @Transactional
    public CourseOutput execute(UpdateCourseInput input) {
        Course course = courseRepository.findById(input.courseId())
                .orElseThrow(() -> new CourseNotFoundException(input.courseId()));

        authorizationService.authorizeEditOrDelete(course, input.requesterId(), input.requesterRole());

        List<CourseSection> domainSections = new ArrayList<>();
        if (input.sections() != null) {
            for (CourseSectionDto sectionDto : input.sections()) {
                List<CourseLesson> domainLessons = new ArrayList<>();
                if (sectionDto.lessons() != null) {
                    for (CourseLessonDto lessonDto : sectionDto.lessons()) {
                        domainLessons.add(CourseLesson.create(
                                lessonDto.title(),
                                lessonDto.contentUrl(),
                                lessonDto.orderIndex()));
                    }
                }
                domainSections.add(CourseSection.create(
                        sectionDto.title(),
                        sectionDto.orderIndex(),
                        domainLessons));
            }
        }

        // Tạo 1 Course mới với id cũ để update toàn phần
        Course updatedCourse = Course.reconstitute(
                course.getId(),
                input.title(),
                input.description(),
                input.maxStudents(),
                course.getEnrolledCount(),
                input.price(),
                course.getInstructorId(),
                domainSections);

        Course savedCourse = courseRepository.save(updatedCourse);
        return CourseOutput.from(savedCourse);
    }
}
