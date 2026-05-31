package com.example.learning_system_spring.application.usecase.Progress;

import com.example.learning_system_spring.application.dto.Progress.CourseProgressOutput;
import com.example.learning_system_spring.application.repository.Course.CourseLessonRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.repository.Course.EnrollmentRepository;
import com.example.learning_system_spring.application.repository.Course.LessonProgressRepository;
import com.example.learning_system_spring.domain.exception.LessonAccessDeniedException;
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
import static org.mockito.Mockito.when;

class GetCourseProgressUseCaseTest {

    @Mock private CourseRepository courseRepository;
    @Mock private CourseLessonRepository lessonRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private LessonProgressRepository lessonProgressRepository;
    @InjectMocks private GetCourseProgressUseCase useCase;

    private final Role memberRole = Role.reconstitute(1L, "MEMBER", null);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Course publishedCourse() {
        return Course.reconstitute(7L, "T", "d", 100, 0, new BigDecimal("100"), 2L,
                null, true, true, LocalDateTime.now(), 1L, List.of());
    }

    private void enrolledMember() {
        when(courseRepository.findById(7L)).thenReturn(Optional.of(publishedCourse()));
        when(enrollmentRepository.existsByUserIdAndCourseId(5L, 7L)).thenReturn(true);
    }

    @Test
    void execute_ZeroLessons_ZeroPercent() {
        enrolledMember();
        when(lessonRepository.countByCourseId(7L)).thenReturn(0L);
        when(lessonProgressRepository.findCompletedLessonIds(5L, 7L)).thenReturn(List.of());

        CourseProgressOutput out = useCase.execute(5L, memberRole, 7L);

        assertEquals(0, out.totalLessons());
        assertEquals(0, out.completedLessons());
        assertEquals(0, out.progressPercent());
    }

    @Test
    void execute_PartialProgress_75Percent() {
        enrolledMember();
        when(lessonRepository.countByCourseId(7L)).thenReturn(4L);
        when(lessonProgressRepository.findCompletedLessonIds(5L, 7L)).thenReturn(List.of(1L, 2L, 3L));

        CourseProgressOutput out = useCase.execute(5L, memberRole, 7L);

        assertEquals(4, out.totalLessons());
        assertEquals(3, out.completedLessons());
        assertEquals(75, out.progressPercent());
        assertEquals(List.of(1L, 2L, 3L), out.completedLessonIds());
    }

    @Test
    void execute_AllCompleted_100Percent() {
        enrolledMember();
        when(lessonRepository.countByCourseId(7L)).thenReturn(2L);
        when(lessonProgressRepository.findCompletedLessonIds(5L, 7L)).thenReturn(List.of(1L, 2L));

        CourseProgressOutput out = useCase.execute(5L, memberRole, 7L);

        assertEquals(100, out.progressPercent());
    }

    @Test
    void execute_NotEnrolled_Denied() {
        when(courseRepository.findById(7L)).thenReturn(Optional.of(publishedCourse()));
        when(enrollmentRepository.existsByUserIdAndCourseId(5L, 7L)).thenReturn(false);

        assertThrows(LessonAccessDeniedException.class,
                () -> useCase.execute(5L, memberRole, 7L));
    }
}
