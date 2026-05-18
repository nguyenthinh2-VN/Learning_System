package com.example.learning_system_spring.application.usecase.Course;

import com.example.learning_system_spring.application.dto.Course.CourseLessonDto;
import com.example.learning_system_spring.application.dto.Course.CourseOutput;
import com.example.learning_system_spring.application.dto.Course.CourseSectionDto;
import com.example.learning_system_spring.application.dto.Course.CreateCourseInput;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.usecase.strategy.course.CourseManagementStrategy;
import com.example.learning_system_spring.application.usecase.strategy.course.CourseStrategyFactory;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.CourseLesson;
import com.example.learning_system_spring.domain.model.CourseSection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateCourseUseCase {

    private final CourseRepository courseRepository;
    private final CourseStrategyFactory strategyFactory;

    @Transactional
    public CourseOutput execute(CreateCourseInput input) {
        CourseManagementStrategy strategy = strategyFactory.getStrategy(input.requesterRole());
        Long instructorId = strategy.resolveInstructorId(input.requesterId(), input.requestedInstructorId());

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

        Course course = Course.create(
                input.title(),
                input.description(),
                input.maxStudents(),
                input.price(),
                instructorId,
                domainSections);

        Course savedCourse = courseRepository.save(course);
        return CourseOutput.from(savedCourse);
    }
}
