package com.example.learning_system_spring.application.usecase.Lesson;

import com.example.learning_system_spring.application.dto.Lesson.GetLessonsInput;
import com.example.learning_system_spring.application.dto.Lesson.GetLessonsOutput;
import com.example.learning_system_spring.application.repository.Course.CourseLessonRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.repository.Course.EnrollmentRepository;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.exception.LessonAccessDeniedException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.CourseLesson;
import com.example.learning_system_spring.domain.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DisplayName("GetLessonsUseCase")
class GetLessonsUseCaseTest {

    @Mock private CourseRepository courseRepository;
    @Mock private CourseLessonRepository lessonRepository;
    @Mock private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private GetLessonsUseCase useCase;

    private static final Long COURSE_ID = 1L;
    private static final Long SECTION_ID = 2L;
    private static final Long INSTRUCTOR_ID = 10L;
    private static final Long MEMBER_ID = 20L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Course makeCourse() {
        return Course.reconstitute(
                COURSE_ID, "Test", "desc", 100, 0,
                new BigDecimal("500000"), INSTRUCTOR_ID,
                true, false, null, null,
                List.of());
    }

    private Role role(String name) {
        return Role.reconstitute(1L, name, null);
    }

    private GetLessonsInput input(Long requesterId, Role role) {
        return new GetLessonsInput(COURSE_ID, SECTION_ID, requesterId, role);
    }

    // ─────────────────────────────────────────────────────────────
    // 404 trước 403
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("course không tồn tại → CourseNotFoundException (404 trước 403)")
    void courseNotFound() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(input(MEMBER_ID, role("MEMBER"))))
                .isInstanceOf(CourseNotFoundException.class);
    }

    // ─────────────────────────────────────────────────────────────
    // SUPER_ADMIN / STAFF — luôn được phép
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("SUPER_ADMIN và STAFF")
    class PrivilegedRoles {

        @Test
        @DisplayName("SUPER_ADMIN → trả danh sách lesson")
        void superAdminAllowed() {
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(makeCourse()));
            when(lessonRepository.findBySectionId(SECTION_ID)).thenReturn(List.of());

            GetLessonsOutput output = useCase.execute(input(99L, role("SUPER_ADMIN")));
            assertThat(output.lessons()).isEmpty();
        }

        @Test
        @DisplayName("STAFF → trả danh sách lesson")
        void staffAllowed() {
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(makeCourse()));
            when(lessonRepository.findBySectionId(SECTION_ID)).thenReturn(List.of());

            GetLessonsOutput output = useCase.execute(input(99L, role("STAFF")));
            assertThat(output.lessons()).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // INSTRUCTOR
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("INSTRUCTOR")
    class InstructorAccess {

        @Test
        @DisplayName("chủ sở hữu → được phép")
        void ownerAllowed() {
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(makeCourse()));
            when(lessonRepository.findBySectionId(SECTION_ID)).thenReturn(List.of());

            GetLessonsOutput output = useCase.execute(input(INSTRUCTOR_ID, role("INSTRUCTOR")));
            assertThat(output).isNotNull();
        }

        @Test
        @DisplayName("không phải chủ sở hữu → LessonAccessDeniedException")
        void nonOwnerDenied() {
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(makeCourse()));

            assertThatThrownBy(() -> useCase.execute(input(99L, role("INSTRUCTOR"))))
                    .isInstanceOf(LessonAccessDeniedException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // MEMBER
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("MEMBER")
    class MemberAccess {

        @Test
        @DisplayName("đã enrolled → được phép, trả danh sách lesson")
        void enrolledMemberAllowed() {
            CourseLesson lesson = CourseLesson.reconstitute(1L, "Bài 1", "url", 1);
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(makeCourse()));
            when(enrollmentRepository.existsByUserIdAndCourseId(MEMBER_ID, COURSE_ID)).thenReturn(true);
            when(lessonRepository.findBySectionId(SECTION_ID)).thenReturn(List.of(lesson));

            GetLessonsOutput output = useCase.execute(input(MEMBER_ID, role("MEMBER")));

            assertThat(output.lessons()).hasSize(1);
            assertThat(output.courseId()).isEqualTo(COURSE_ID);
            assertThat(output.sectionId()).isEqualTo(SECTION_ID);
        }

        @Test
        @DisplayName("chưa enrolled → LessonAccessDeniedException")
        void notEnrolledDenied() {
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(makeCourse()));
            when(enrollmentRepository.existsByUserIdAndCourseId(MEMBER_ID, COURSE_ID)).thenReturn(false);

            assertThatThrownBy(() -> useCase.execute(input(MEMBER_ID, role("MEMBER"))))
                    .isInstanceOf(LessonAccessDeniedException.class)
                    .hasMessageContaining("chưa đăng ký");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ADMIN_USER
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("ADMIN_USER → LessonAccessDeniedException")
    void adminUserDenied() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(makeCourse()));

        assertThatThrownBy(() -> useCase.execute(input(99L, role("ADMIN_USER"))))
                .isInstanceOf(LessonAccessDeniedException.class);
    }
}
