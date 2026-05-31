package com.example.learning_system_spring.application.usecase.Progress;

import com.example.learning_system_spring.application.repository.Course.CourseLessonRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.repository.Course.EnrollmentRepository;
import com.example.learning_system_spring.application.repository.Course.LessonProgressRepository;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

class UnmarkLessonCompleteUseCaseTest {

    @Mock private CourseRepository courseRepository;
    @Mock private CourseLessonRepository lessonRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private LessonProgressRepository lessonProgressRepository;
    @InjectMocks private UnmarkLessonCompleteUseCase useCase;

    private final Role memberRole = Role.reconstitute(1L, "MEMBER", null);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Course publishedCourse() {
        return Course.reconstitute(7L, "T", "d", 100, 0, new BigDecimal("100"), 2L,
                null, true, true, LocalDateTime.now(), 1L, List.of());
    }

    @Test
    void execute_UnmarksSuccessfully() {
        when(courseRepository.findById(7L)).thenReturn(Optional.of(publishedCourse()));
        when(enrollmentRepository.existsByUserIdAndCourseId(5L, 7L)).thenReturn(true);
        when(lessonRepository.existsByIdAndCourseId(3L, 7L)).thenReturn(true);

        useCase.execute(5L, memberRole, 7L, 3L);

        verify(lessonProgressRepository).unmarkComplete(5L, 3L);
    }

    @Test
    void execute_NotMarkedYet_NoError() {
        when(courseRepository.findById(7L)).thenReturn(Optional.of(publishedCourse()));
        when(enrollmentRepository.existsByUserIdAndCourseId(5L, 7L)).thenReturn(true);
        when(lessonRepository.existsByIdAndCourseId(3L, 7L)).thenReturn(true);
        // unmarkComplete là no-op nếu chưa có — không ném lỗi
        doNothing().when(lessonProgressRepository).unmarkComplete(5L, 3L);

        useCase.execute(5L, memberRole, 7L, 3L);

        verify(lessonProgressRepository).unmarkComplete(5L, 3L);
    }
}
