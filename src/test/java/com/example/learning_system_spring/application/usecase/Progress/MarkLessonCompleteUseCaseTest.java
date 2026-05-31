package com.example.learning_system_spring.application.usecase.Progress;

import com.example.learning_system_spring.application.repository.Course.CourseLessonRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.repository.Course.EnrollmentRepository;
import com.example.learning_system_spring.application.repository.Course.LessonProgressRepository;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.exception.LessonAccessDeniedException;
import com.example.learning_system_spring.domain.exception.LessonNotFoundException;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MarkLessonCompleteUseCaseTest {

    @Mock private CourseRepository courseRepository;
    @Mock private CourseLessonRepository lessonRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private LessonProgressRepository lessonProgressRepository;
    @InjectMocks private MarkLessonCompleteUseCase useCase;

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
    void execute_EnrolledMember_MarksComplete() {
        when(courseRepository.findById(7L)).thenReturn(Optional.of(publishedCourse()));
        when(enrollmentRepository.existsByUserIdAndCourseId(5L, 7L)).thenReturn(true);
        when(lessonRepository.existsByIdAndCourseId(3L, 7L)).thenReturn(true);
        when(lessonProgressRepository.existsByUserIdAndLessonId(5L, 3L)).thenReturn(false);

        useCase.execute(5L, memberRole, 7L, 3L);

        verify(lessonProgressRepository).markComplete(any());
    }

    @Test
    void execute_Idempotent_AlreadyCompleted_NoDuplicate() {
        when(courseRepository.findById(7L)).thenReturn(Optional.of(publishedCourse()));
        when(enrollmentRepository.existsByUserIdAndCourseId(5L, 7L)).thenReturn(true);
        when(lessonRepository.existsByIdAndCourseId(3L, 7L)).thenReturn(true);
        when(lessonProgressRepository.existsByUserIdAndLessonId(5L, 3L)).thenReturn(true);

        useCase.execute(5L, memberRole, 7L, 3L);

        verify(lessonProgressRepository, never()).markComplete(any());
    }

    @Test
    void execute_NotEnrolled_Denied() {
        when(courseRepository.findById(7L)).thenReturn(Optional.of(publishedCourse()));
        when(enrollmentRepository.existsByUserIdAndCourseId(5L, 7L)).thenReturn(false);

        assertThrows(LessonAccessDeniedException.class,
                () -> useCase.execute(5L, memberRole, 7L, 3L));
        verify(lessonProgressRepository, never()).markComplete(any());
    }

    @Test
    void execute_LessonNotInCourse_NotFound() {
        when(courseRepository.findById(7L)).thenReturn(Optional.of(publishedCourse()));
        when(enrollmentRepository.existsByUserIdAndCourseId(5L, 7L)).thenReturn(true);
        when(lessonRepository.existsByIdAndCourseId(999L, 7L)).thenReturn(false);

        assertThrows(LessonNotFoundException.class,
                () -> useCase.execute(5L, memberRole, 7L, 999L));
        verify(lessonProgressRepository, never()).markComplete(any());
    }

    @Test
    void execute_CourseNotFound() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CourseNotFoundException.class,
                () -> useCase.execute(5L, memberRole, 99L, 3L));
    }
}
