package com.example.learning_system_spring.application.usecase.Progress;

import com.example.learning_system_spring.application.repository.Course.CourseLessonRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.repository.Course.EnrollmentRepository;
import com.example.learning_system_spring.application.repository.Course.LessonProgressRepository;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.exception.LessonNotFoundException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.LessonProgress;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.service.LessonAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đánh dấu một lesson đã hoàn thành cho người gọi. Idempotent.
 */
@Service
@RequiredArgsConstructor
public class MarkLessonCompleteUseCase {

    private final CourseRepository courseRepository;
    private final CourseLessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;

    @Transactional
    public void execute(Long requesterId, Role requesterRole, Long courseId, Long lessonId) {
        // 404 trước 403
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        boolean isEnrolled = requesterRole.isMember()
                && enrollmentRepository.existsByUserIdAndCourseId(requesterId, courseId);
        LessonAuthorizationService.authorizeView(course, requesterId, requesterRole, isEnrolled);

        // Lesson phải thuộc course này
        if (!lessonRepository.existsByIdAndCourseId(lessonId, courseId)) {
            throw new LessonNotFoundException(lessonId);
        }

        // Idempotent: chỉ tạo nếu chưa có
        if (!lessonProgressRepository.existsByUserIdAndLessonId(requesterId, lessonId)) {
            lessonProgressRepository.markComplete(
                    LessonProgress.create(requesterId, lessonId, courseId));
        }
    }
}
