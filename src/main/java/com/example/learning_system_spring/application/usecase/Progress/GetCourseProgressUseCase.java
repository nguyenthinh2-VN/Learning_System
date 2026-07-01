package com.example.learning_system_spring.application.usecase.Progress;

import com.example.learning_system_spring.application.dto.Progress.CourseProgressOutput;
import com.example.learning_system_spring.application.repository.Course.CourseLessonRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.repository.Course.EnrollmentRepository;
import com.example.learning_system_spring.application.repository.Course.LessonProgressRepository;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.service.LessonAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tiến độ học của người gọi trong một course: tổng lesson, số đã hoàn thành, %.
 */
@Service
@RequiredArgsConstructor
public class GetCourseProgressUseCase {

        private final CourseRepository courseRepository;
        private final CourseLessonRepository lessonRepository;
        private final EnrollmentRepository enrollmentRepository;
        private final LessonProgressRepository lessonProgressRepository;

        @Transactional(readOnly = true)
        public CourseProgressOutput execute(Long requesterId, Role requesterRole, Long courseId) {
                Course course = courseRepository.findById(courseId)
                                .orElseThrow(() -> new CourseNotFoundException(courseId));

                boolean isEnrolled = requesterRole.isMember()
                                && enrollmentRepository.existsByUserIdAndCourseId(requesterId, courseId);
                LessonAuthorizationService.authorizeView(course, requesterId, requesterRole, isEnrolled);

                int totalLessons = (int) lessonRepository.countByCourseId(courseId);
                List<Long> completedIds = lessonProgressRepository.findCompletedLessonIds(requesterId, courseId);
                int completed = completedIds.size();

                int percent = totalLessons == 0
                                ? 0
                                : (int) Math.round((completed * 100.0) / totalLessons);

                return new CourseProgressOutput(courseId, totalLessons, completed, percent, completedIds);
        }
}
