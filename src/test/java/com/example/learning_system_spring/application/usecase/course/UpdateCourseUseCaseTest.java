package com.example.learning_system_spring.application.usecase.course;

import com.example.learning_system_spring.application.dto.Course.CourseOutput;
import com.example.learning_system_spring.application.dto.Course.UpdateCourseInput;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.usecase.Course.UpdateCourseUseCase;
import com.example.learning_system_spring.domain.exception.CourseAccessDeniedException;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.service.CourseAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UpdateCourseUseCaseTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseAuthorizationService authorizationService;

    @InjectMocks
    private UpdateCourseUseCase updateCourseUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_ShouldUpdateCourse_WhenValidAndAuthorized() {
        // Arrange
        Role instructorRole = Role.reconstitute(2L, "INSTRUCTOR", "Giảng viên");
        UpdateCourseInput input = new UpdateCourseInput(
                1L, 100L, instructorRole, "Java 102", "Advanced Java", 100, java.math.BigDecimal.ZERO, null);

        Course existingCourse = Course.reconstitute(1L, "Java 101", "Learn Java", 50, 0, java.math.BigDecimal.ZERO, 100L, null);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(existingCourse));
        doNothing().when(authorizationService).authorizeEditOrDelete(existingCourse, 100L, instructorRole);

        Course updatedCourse = Course.reconstitute(1L, "Java 102", "Advanced Java", 100, 0, java.math.BigDecimal.ZERO, 100L, null);
        when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

        // Act
        CourseOutput output = updateCourseUseCase.execute(input);

        // Assert
        assertNotNull(output);
        assertEquals("Java 102", output.title());
        assertEquals("Advanced Java", output.description());
        assertEquals(100, output.maxStudents());
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void execute_ShouldThrowCourseNotFound_WhenCourseDoesNotExist() {
        // Arrange
        Role instructorRole = Role.reconstitute(2L, "INSTRUCTOR", "Giảng viên");
        UpdateCourseInput input = new UpdateCourseInput(
                99L, 100L, instructorRole, "Java 102", "Advanced Java", 100, java.math.BigDecimal.ZERO, null);

        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CourseNotFoundException.class, () -> updateCourseUseCase.execute(input));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void execute_ShouldThrowAccessDenied_WhenUnauthorized() {
        // Arrange
        Role instructorRole = Role.reconstitute(2L, "INSTRUCTOR", "Giảng viên");
        UpdateCourseInput input = new UpdateCourseInput(
                1L, 200L, instructorRole, "Java 102", "Advanced Java", 100, java.math.BigDecimal.ZERO, null);

        Course existingCourse = Course.reconstitute(1L, "Java 101", "Learn Java", 50, 0, java.math.BigDecimal.ZERO, 100L, null);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(existingCourse));

        doThrow(new CourseAccessDeniedException("Not your course"))
                .when(authorizationService).authorizeEditOrDelete(existingCourse, 200L, instructorRole);

        // Act & Assert
        assertThrows(CourseAccessDeniedException.class, () -> updateCourseUseCase.execute(input));
        verify(courseRepository, never()).save(any());
    }
}
