package com.example.learning_system_spring.application.usecase.Lesson;

import com.example.learning_system_spring.application.dto.Lesson.UpdateLessonInput;
import com.example.learning_system_spring.application.dto.Section.LessonOutput;
import com.example.learning_system_spring.application.repository.Course.CourseLessonRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.repository.Course.CourseSectionRepository;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.exception.LessonNotFoundException;
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
public class UpdateLessonUseCase {
    private final CourseRepository courseRepository;
    private final CourseSectionRepository sectionRepository;
    private final CourseLessonRepository lessonRepository;

    @Transactional
    public LessonOutput execute(UpdateLessonInput input) {
        // Kiểm tra lesson tồn tại
        CourseLesson lesson = lessonRepository.findById(input.lessonId())
                .orElseThrow(() -> new LessonNotFoundException(input.lessonId()));

        // Kiểm tra section tồn tại
        CourseSection section = sectionRepository.findById(input.sectionId())
                .orElseThrow(() -> new SectionNotFoundException(input.sectionId()));

        // Kiểm tra course tồn tại
        Course course = courseRepository.findById(input.courseId())
                .orElseThrow(() -> new CourseNotFoundException(input.courseId()));

        // Kiểm tra quyền
        LessonAuthorizationService.authorizeEditOrDelete(course, input.requesterId(), input.requesterRole());

        // Cập nhật lesson
        CourseLesson updatedLesson = CourseLesson.reconstitute(
                lesson.getId(),
                input.title(),
                input.contentUrl(),
                input.orderIndex()
        );

        // Lưu lesson
        CourseLesson savedLesson = lessonRepository.save(updatedLesson, input.sectionId());

        return LessonOutput.from(savedLesson);
    }
}