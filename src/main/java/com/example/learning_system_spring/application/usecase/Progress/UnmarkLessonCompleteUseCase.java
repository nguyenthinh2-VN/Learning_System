package com.example.learning_system_spring.application.usecase.Progress;

import com.example.learning_system_spring.application.repository.Course.CourseLessonRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.repository.Course.EnrollmentRepository;
import com.example.learning_system_spring.application.repository.Course.LessonProgressRepository;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.exception.LessonNotFoundException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.service.LessonAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bỏ đánh dấu hoàn thành một lesson (học lại). No-op nếu chưa đánh dấu.
 */
@Service
@RequiredArgsConstructor
public class UnmarkLessonCompleteUseCase {

    private final CourseRepository courseRepository;
    private final CourseLessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;

    @Transactional
    public void execute(Long requesterId, Role requesterRole, Long courseId, Long lessonId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        boolean isEnrolled = requesterRole.isMember()
                && enrollmentRepository.existsByUserIdAndCourseId(requesterId, courseId);
        LessonAuthorizationService.authorizeView(course, requesterId, requesterRole, isEnrolled);

        if (!lessonRepository.existsByIdAndCourseId(lessonId, courseId)) {
            throw new LessonNotFoundException(lessonId);
        }

        lessonProgressRepository.unmarkComplete(requesterId, lessonId);
    }
}
