package com.example.learning_system_spring.application.usecase.section;

import com.example.learning_system_spring.application.dto.Section.DeleteSectionInput;
import com.example.learning_system_spring.application.repository.Course.CourseSectionRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.usecase.Section.DeleteSectionUseCase;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DeleteSectionUseCaseTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseSectionRepository sectionRepository;

    @Mock
    private SectionAuthorizationService authorizationService;

    @InjectMocks
    private DeleteSectionUseCase deleteSectionUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_ShouldDeleteSection_WhenInstructorOwnsTheCourse() {
        // Arrange
        Role instructor = Role.reconstitute(2L, "INSTRUCTOR", null);
        CourseSection section = CourseSection.reconstitute(10L, "Chương 1", 1, new ArrayList<>());
        Course course = Course.reconstitute(1L, "Java 101", "Desc", 50, 0, BigDecimal.ZERO, 100L, null);
        DeleteSectionInput input = new DeleteSectionInput(10L, 1L, 100L, instructor);

        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        doNothing().when(authorizationService).authorizeEditOrDelete(course, 100L, instructor);

        // Act
        deleteSectionUseCase.execute(input);

        // Assert
        verify(sectionRepository).deleteById(10L);
    }

    @Test
    void execute_ShouldDeleteSection_WhenStaffRequests() {
        // Arrange
        Role staff = Role.reconstitute(3L, "STAFF", null);
        CourseSection section = CourseSection.reconstitute(10L, "Chương 1", 1, new ArrayList<>());
        Course course = Course.reconstitute(1L, "Java 101", "Desc", 50, 0, BigDecimal.ZERO, 100L, null);
        DeleteSectionInput input = new DeleteSectionInput(10L, 1L, 200L, staff);

        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        doNothing().when(authorizationService).authorizeEditOrDelete(course, 200L, staff);

        // Act
        deleteSectionUseCase.execute(input);

        // Assert
        verify(sectionRepository).deleteById(10L);
    }

    @Test
    void execute_ShouldThrowSectionNotFoundException_WhenSectionDoesNotExist() {
        // Arrange
        Role instructor = Role.reconstitute(2L, "INSTRUCTOR", null);
        DeleteSectionInput input = new DeleteSectionInput(99L, 1L, 100L, instructor);

        when(sectionRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(SectionNotFoundException.class, () -> deleteSectionUseCase.execute(input));
        verify(courseRepository, never()).findById(any());
        verify(sectionRepository, never()).deleteById(any());
    }

    @Test
    void execute_ShouldThrowCourseNotFoundException_WhenCourseDoesNotExist() {
        // Arrange
        Role instructor = Role.reconstitute(2L, "INSTRUCTOR", null);
        CourseSection section = CourseSection.reconstitute(10L, "Chương 1", 1, new ArrayList<>());
        DeleteSectionInput input = new DeleteSectionInput(10L, 99L, 100L, instructor);

        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CourseNotFoundException.class, () -> deleteSectionUseCase.execute(input));
        verify(sectionRepository, never()).deleteById(any());
    }

    @Test
    void execute_ShouldThrowSectionAccessDeniedException_WhenInstructorDoesNotOwnCourse() {
        // Arrange
        Role instructor = Role.reconstitute(2L, "INSTRUCTOR", null);
        CourseSection section = CourseSection.reconstitute(10L, "Chương 1", 1, new ArrayList<>());
        Course course = Course.reconstitute(1L, "Java 101", "Desc", 50, 0, BigDecimal.ZERO, 100L, null);
        DeleteSectionInput input = new DeleteSectionInput(10L, 1L, 999L, instructor);

        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        doThrow(new SectionAccessDeniedException("Giảng viên chỉ có quyền sửa/xóa chương học trong khóa học do chính mình tạo."))
                .when(authorizationService).authorizeEditOrDelete(course, 999L, instructor);

        // Act & Assert
        assertThrows(SectionAccessDeniedException.class, () -> deleteSectionUseCase.execute(input));
        verify(sectionRepository, never()).deleteById(any());
    }

    @Test
    void execute_ShouldThrowSectionAccessDeniedException_WhenAdminUserRequests() {
        // Arrange
        Role adminUser = Role.reconstitute(4L, "ADMIN_USER", null);
        CourseSection section = CourseSection.reconstitute(10L, "Chương 1", 1, new ArrayList<>());
        Course course = Course.reconstitute(1L, "Java 101", "Desc", 50, 0, BigDecimal.ZERO, 100L, null);
        DeleteSectionInput input = new DeleteSectionInput(10L, 1L, 300L, adminUser);

        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        doThrow(new SectionAccessDeniedException("Bạn không có quyền sửa/xóa chương học."))
                .when(authorizationService).authorizeEditOrDelete(course, 300L, adminUser);

        // Act & Assert
        assertThrows(SectionAccessDeniedException.class, () -> deleteSectionUseCase.execute(input));
        verify(sectionRepository, never()).deleteById(any());
    }
}
