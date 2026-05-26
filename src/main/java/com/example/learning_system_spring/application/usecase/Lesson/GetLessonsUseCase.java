package com.example.learning_system_spring.application.usecase.Lesson;

import com.example.learning_system_spring.application.dto.Lesson.GetLessonsInput;
import com.example.learning_system_spring.application.dto.Lesson.GetLessonsOutput;
import com.example.learning_system_spring.application.dto.Section.LessonOutput;
import com.example.learning_system_spring.application.repository.Course.CourseLessonRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.repository.Course.EnrollmentRepository;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.CourseLesson;
import com.example.learning_system_spring.domain.service.LessonAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetLessonsUseCase {
    private final CourseRepository courseRepository;
    private final CourseLessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public GetLessonsOutput execute(GetLessonsInput input) {
        // 404 trước 403 — kiểm tra course tồn tại trước
        Course course = courseRepository.findById(input.courseId())
                .orElseThrow(() -> new CourseNotFoundException(input.courseId()));

        // Kiểm tra enrollment cho MEMBER (1 query boolean, không N+1)
        boolean isEnrolled = input.requesterRole().isMember()
                && enrollmentRepository.existsByUserIdAndCourseId(input.requesterId(), input.courseId());

        // Kiểm soát truy cập — ném LessonAccessDeniedException nếu không có quyền
        LessonAuthorizationService.authorizeView(course, input.requesterId(), input.requesterRole(), isEnrolled);

        List<CourseLesson> lessons = lessonRepository.findBySectionId(input.sectionId());
        List<LessonOutput> lessonOutputs = lessons.stream()
                .map(LessonOutput::from)
                .toList();

        return GetLessonsOutput.of(input.courseId(), input.sectionId(), lessonOutputs);
    }
}