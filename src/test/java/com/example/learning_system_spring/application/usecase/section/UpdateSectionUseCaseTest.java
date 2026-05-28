package com.example.learning_system_spring.application.usecase.section;

import com.example.learning_system_spring.application.dto.Section.SectionOutput;
import com.example.learning_system_spring.application.dto.Section.UpdateSectionInput;
import com.example.learning_system_spring.application.repository.Course.CourseSectionRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.usecase.Section.UpdateSectionUseCase;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.exception.SectionAccessDeniedException;
import com.example.learning_system_spring.domain.exception.SectionNotFoundException;
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

class UpdateSectionUseCaseTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseSectionRepository sectionRepository;

    @Mock
    private SectionAuthorizationService authorizationService;

    @InjectMocks
    private UpdateSectionUseCase updateSectionUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_ShouldUpdateSection_WhenInstructorOwnsTheCourse() {
        // Arrange
        Role instructor = Role.reconstitute(2L, "INSTRUCTOR", null);
        CourseSection existing = CourseSection.reconstitute(10L, "Chương 1 cũ", 1, new ArrayList<>());
        Course course = Course.reconstitute(1L, "Java 101", "Desc", 50, 0, BigDecimal.ZERO, 100L, null, true, true, null, null, java.util.List.of());
        UpdateSectionInput input = new UpdateSectionInput(10L, 1L, 100L, instructor, "Chương 1 mới", 2);

        when(sectionRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        doNothing().when(authorizationService).authorizeEditOrDelete(course, 100L, instructor);

        CourseSection saved = CourseSection.reconstitute(10L, "Chương 1 mới", 2, new ArrayList<>());
        when(sectionRepository.save(any(CourseSection.class), eq(1L))).thenReturn(saved);

        // Act
        SectionOutput output = updateSectionUseCase.execute(input);

        // Assert
        assertNotNull(output);
        assertEquals("Chương 1 mới", output.title());
        assertEquals(2, output.orderIndex());
        verify(sectionRepository).save(any(CourseSection.class), eq(1L));
    }

    @Test
    void execute_ShouldUpdateSection_WhenSuperAdminRequests() {
        // Arrange
        Role superAdmin = Role.reconstitute(5L, "SUPER_ADMIN", null);
        CourseSection existing = CourseSection.reconstitute(10L, "Chương 1", 1, new ArrayList<>());
        Course course = Course.reconstitute(1L, "Java 101", "Desc", 50, 0, BigDecimal.ZERO, 100L, null, true, true, null, null, java.util.List.of());
        UpdateSectionInput input = new UpdateSectionInput(10L, 1L, 500L, superAdmin, "Chương 1 Updated", 1);

        when(sectionRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        doNothing().when(authorizationService).authorizeEditOrDelete(course, 500L, superAdmin);

        CourseSection saved = CourseSection.reconstitute(10L, "Chương 1 Updated", 1, new ArrayList<>());
        when(sectionRepository.save(any(CourseSection.class), eq(1L))).thenReturn(saved);

        // Act
        SectionOutput output = updateSectionUseCase.execute(input);

        // Assert
        assertEquals("Chương 1 Updated", output.title());
    }

    @Test
    void execute_ShouldThrowSectionNotFoundException_WhenSectionDoesNotExist() {
        // Arrange
        Role instructor = Role.reconstitute(2L, "INSTRUCTOR", null);
        UpdateSectionInput input = new UpdateSectionInput(99L, 1L, 100L, instructor, "Chương X", 1);

        when(sectionRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(SectionNotFoundException.class, () -> updateSectionUseCase.execute(input));
        verify(courseRepository, never()).findById(any());
        verify(sectionRepository, never()).save(any(), any());
    }

    @Test
    void execute_ShouldThrowCourseNotFoundException_WhenCourseDoesNotExist() {
        // Arrange
        Role instructor = Role.reconstitute(2L, "INSTRUCTOR", null);
        CourseSection existing = CourseSection.reconstitute(10L, "Chương 1", 1, new ArrayList<>());
        UpdateSectionInput input = new UpdateSectionInput(10L, 99L, 100L, instructor, "Chương 1 mới", 1);

        when(sectionRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CourseNotFoundException.class, () -> updateSectionUseCase.execute(input));
        verify(sectionRepository, never()).save(any(), any());
    }

    @Test
    void execute_ShouldThrowSectionAccessDeniedException_WhenInstructorDoesNotOwnCourse() {
        // Arrange
        Role instructor = Role.reconstitute(2L, "INSTRUCTOR", null);
        CourseSection existing = CourseSection.reconstitute(10L, "Chương 1", 1, new ArrayList<>());
        Course course = Course.reconstitute(1L, "Java 101", "Desc", 50, 0, BigDecimal.ZERO, 100L, null, true, true, null, null, java.util.List.of());
        UpdateSectionInput input = new UpdateSectionInput(10L, 1L, 999L, instructor, "Chương 1 mới", 1);

        when(sectionRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        doThrow(new SectionAccessDeniedException("Giảng viên chỉ có quyền sửa/xóa chương học trong khóa học do chính mình tạo."))
                .when(authorizationService).authorizeEditOrDelete(course, 999L, instructor);

        // Act & Assert
        assertThrows(SectionAccessDeniedException.class, () -> updateSectionUseCase.execute(input));
        verify(sectionRepository, never()).save(any(), any());
    }

    @Test
    void execute_ShouldThrowSectionAccessDeniedException_WhenAdminUserRequests() {
        // Arrange
        Role adminUser = Role.reconstitute(4L, "ADMIN_USER", null);
        CourseSection existing = CourseSection.reconstitute(10L, "Chương 1", 1, new ArrayList<>());
        Course course = Course.reconstitute(1L, "Java 101", "Desc", 50, 0, BigDecimal.ZERO, 100L, null, true, true, null, null, java.util.List.of());
        UpdateSectionInput input = new UpdateSectionInput(10L, 1L, 300L, adminUser, "Chương 1 mới", 1);

        when(sectionRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        doThrow(new SectionAccessDeniedException("Bạn không có quyền sửa/xóa chương học."))
                .when(authorizationService).authorizeEditOrDelete(course, 300L, adminUser);

        // Act & Assert
        assertThrows(SectionAccessDeniedException.class, () -> updateSectionUseCase.execute(input));
    }
}
