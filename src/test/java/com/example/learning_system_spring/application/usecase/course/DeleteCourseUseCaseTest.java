package com.example.learning_system_spring.application.usecase.course;

import com.example.learning_system_spring.application.dto.Course.DeleteCourseInput;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.usecase.Course.DeleteCourseUseCase;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DeleteCourseUseCaseTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseAuthorizationService authorizationService;

    @InjectMocks
    private DeleteCourseUseCase deleteCourseUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_ShouldDeleteCourse_WhenValidAndAuthorized() {
        // Arrange
        Role instructorRole = Role.reconstitute(2L, "INSTRUCTOR", "Giảng viên");
        DeleteCourseInput input = new DeleteCourseInput(1L, 100L, instructorRole);

        Course existingCourse = Course.reconstitute(1L, "Java 101", "Learn Java", 50, 0, java.math.BigDecimal.ZERO, 100L, null);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(existingCourse));
        doNothing().when(authorizationService).authorizeEditOrDelete(existingCourse, 100L, instructorRole);

        // Act
        deleteCourseUseCase.execute(input);

        // Assert
        verify(courseRepository).deleteById(1L);
    }

    @Test
    void execute_ShouldThrowCourseNotFound_WhenCourseDoesNotExist() {
        // Arrange
        Role instructorRole = Role.reconstitute(2L, "INSTRUCTOR", "Giảng viên");
        DeleteCourseInput input = new DeleteCourseInput(99L, 100L, instructorRole);

        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CourseNotFoundException.class, () -> deleteCourseUseCase.execute(input));
        verify(courseRepository, never()).deleteById(any());
    }

    @Test
    void execute_ShouldThrowAccessDenied_WhenUnauthorized() {
        // Arrange
        Role instructorRole = Role.reconstitute(2L, "INSTRUCTOR", "Giảng viên");
        DeleteCourseInput input = new DeleteCourseInput(1L, 200L, instructorRole);

        Course existingCourse = Course.reconstitute(1L, "Java 101", "Learn Java", 50, 0, java.math.BigDecimal.ZERO, 100L, null);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(existingCourse));

        doThrow(new CourseAccessDeniedException("Not your course"))
                .when(authorizationService).authorizeEditOrDelete(existingCourse, 200L, instructorRole);

        // Act & Assert
        assertThrows(CourseAccessDeniedException.class, () -> deleteCourseUseCase.execute(input));
        verify(courseRepository, never()).deleteById(any());
    }
}
