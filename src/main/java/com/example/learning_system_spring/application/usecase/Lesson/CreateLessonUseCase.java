package com.example.learning_system_spring.application.usecase.Lesson;

import com.example.learning_system_spring.application.dto.Lesson.CreateLessonInput;
import com.example.learning_system_spring.application.dto.Section.LessonOutput;
import com.example.learning_system_spring.application.repository.Course.CourseLessonRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.repository.Course.CourseSectionRepository;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.exception.SectionNotFoundException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.CourseLesson;
import com.example.learning_system_spring.domain.model.CourseSection;
import com.example.learning_system_spring.domain.service.LessonAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateLessonUseCase {
    private final CourseRepository courseRepository;
    private final CourseSectionRepository sectionRepository;
    private final CourseLessonRepository lessonRepository;

    @Transactional
    public LessonOutput execute(CreateLessonInput input) {
        // Kiểm tra course tồn tại
        Course course = courseRepository.findById(input.courseId())
                .orElseThrow(() -> new CourseNotFoundException(input.courseId()));

        // Kiểm tra section tồn tại
        CourseSection section = sectionRepository.findById(input.sectionId())
                .orElseThrow(() -> new SectionNotFoundException(input.sectionId()));

        // Kiểm tra quyền
        LessonAuthorizationService.authorizeCreate(course, input.requesterId(), input.requesterRole());

        // Tạo lesson
        CourseLesson lesson = CourseLesson.create(
                input.title(),
                input.contentUrl(),
                input.orderIndex()
        );

        // Lưu lesson
        CourseLesson savedLesson = lessonRepository.save(lesson, input.sectionId());

        return LessonOutput.from(savedLesson);
    }
}