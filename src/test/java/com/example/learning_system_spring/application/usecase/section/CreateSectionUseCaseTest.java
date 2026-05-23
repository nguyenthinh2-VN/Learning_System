package com.example.learning_system_spring.application.usecase.section;

import com.example.learning_system_spring.application.dto.Section.CreateSectionInput;
import com.example.learning_system_spring.application.dto.Section.SectionOutput;
import com.example.learning_system_spring.application.repository.Course.CourseSectionRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.usecase.Section.CreateSectionUseCase;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.exception.SectionAccessDeniedException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.CourseSection;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.service.SectionAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CreateSectionUseCaseTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseSectionRepository sectionRepository;

    @Mock
    private SectionAuthorizationService authorizationService;

    @InjectMocks
    private CreateSectionUseCase createSectionUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_ShouldCreateSection_WhenInstructorOwnsTheCourse() {
        // Arrange
        Role instructor = Role.reconstitute(2L, "INSTRUCTOR", null);
        Course course = Course.reconstitute(1L, "Java 101", "Desc", 50, 0, BigDecimal.ZERO, 100L, null);
        CreateSectionInput input = new CreateSectionInput(1L, 100L, instructor, "Chương 1: Giới thiệu", 1);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        doNothing().when(authorizationService).authorizeCreate(course, 100L, instructor);

        CourseSection saved = CourseSection.reconstitute(10L, "Chương 1: Giới thiệu", 1, new ArrayList<>());
        when(sectionRepository.save(any(CourseSection.class), eq(1L))).thenReturn(saved);

        // Act
        SectionOutput output = createSectionUseCase.execute(input);

        // Assert
        assertNotNull(output);
        assertEquals(10L, output.id());
        assertEquals("Chương 1: Giới thiệu", output.title());
        assertEquals(1, output.orderIndex());
        assertTrue(output.lessons().isEmpty());
        verify(sectionRepository).save(any(CourseSection.class), eq(1L));
    }

    @Test
    void execute_ShouldCreateSection_WhenStaffRequests() {
        // Arrange
        Role staff = Role.reconstitute(3L, "STAFF", null);
        Course course = Course.reconstitute(1L, "Java 101", "Desc", 50, 0, BigDecimal.ZERO, 100L, null);
        CreateSectionInput input = new CreateSectionInput(1L, 200L, staff, "Chương 2", 2);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        doNothing().when(authorizationService).authorizeCreate(course, 200L, staff);

        CourseSection saved = CourseSection.reconstitute(11L, "Chương 2", 2, new ArrayList<>());
        when(sectionRepository.save(any(CourseSection.class), eq(1L))).thenReturn(saved);

        // Act
        SectionOutput output = createSectionUseCase.execute(input);

        // Assert
        assertNotNull(output);
        assertEquals("Chương 2", output.title());
    }

    @Test
    void execute_ShouldThrowCourseNotFoundException_WhenCourseDoesNotExist() {
        // Arrange
        Role instructor = Role.reconstitute(2L, "INSTRUCTOR", null);
        CreateSectionInput input = new CreateSectionInput(99L, 100L, instructor, "Chương 1", 1);

        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CourseNotFoundException.class, () -> createSectionUseCase.execute(input));
        verify(sectionRepository, never()).save(any(), any());
    }

    @Test
    void execute_ShouldThrowSectionAccessDeniedException_WhenInstructorDoesNotOwnCourse() {
        // Arrange
        Role instructor = Role.reconstitute(2L, "INSTRUCTOR", null);
        Course course = Course.reconstitute(1L, "Java 101", "Desc", 50, 0, BigDecimal.ZERO, 100L, null);
        CreateSectionInput input = new CreateSectionInput(1L, 999L, instructor, "Chương 1", 1); // 999L != 100L

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        doThrow(new SectionAccessDeniedException("Giảng viên chỉ có quyền thêm chương học trong khóa học do chính mình tạo."))
                .when(authorizationService).authorizeCreate(course, 999L, instructor);

        // Act & Assert
        assertThrows(SectionAccessDeniedException.class, () -> createSectionUseCase.execute(input));
        verify(sectionRepository, never()).save(any(), any());
    }

    @Test
    void execute_ShouldThrowSectionAccessDeniedException_WhenAdminUserRequests() {
        // Arrange
        Role adminUser = Role.reconstitute(4L, "ADMIN_USER", null);
        Course course = Course.reconstitute(1L, "Java 101", "Desc", 50, 0, BigDecimal.ZERO, 100L, null);
        CreateSectionInput input = new CreateSectionInput(1L, 300L, adminUser, "Chương 1", 1);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        doThrow(new SectionAccessDeniedException("Bạn không có quyền thêm chương học."))
                .when(authorizationService).authorizeCreate(course, 300L, adminUser);

        // Act & Assert
        SectionAccessDeniedException ex = assertThrows(SectionAccessDeniedException.class,
                () -> createSectionUseCase.execute(input));
        assertEquals("Bạn không có quyền thêm chương học.", ex.getMessage());
    }

    @Test
    void execute_ShouldThrowSectionAccessDeniedException_WhenMemberRequests() {
        // Arrange
        Role member = Role.reconstitute(1L, "MEMBER", null);
        Course course = Course.reconstitute(1L, "Java 101", "Desc", 50, 0, BigDecimal.ZERO, 100L, null);
        CreateSectionInput input = new CreateSectionInput(1L, 50L, member, "Chương 1", 1);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        doThrow(new SectionAccessDeniedException("Bạn không có quyền thêm chương học."))
                .when(authorizationService).authorizeCreate(course, 50L, member);

        // Act & Assert
        assertThrows(SectionAccessDeniedException.class, () -> createSectionUseCase.execute(input));
    }
}
