package com.example.learning_system_spring.application.usecase.section;

import com.example.learning_system_spring.application.dto.Section.SectionOutput;
import com.example.learning_system_spring.application.repository.Course.CourseSectionRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.usecase.Section.GetSectionsUseCase;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.CourseSection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetSectionsUseCaseTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseSectionRepository sectionRepository;

    @InjectMocks
    private GetSectionsUseCase getSectionsUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_ShouldReturnSections_WhenCourseExists() {
        // Arrange
        Course course = Course.reconstitute(1L, "Java 101", "Desc", 50, 0, BigDecimal.ZERO, 100L, true, true, null, null, java.util.List.of());
        List<CourseSection> sections = List.of(
                CourseSection.reconstitute(1L, "Chương 1", 1, new ArrayList<>()),
                CourseSection.reconstitute(2L, "Chương 2", 2, new ArrayList<>())
        );

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(sectionRepository.findByCourseId(1L)).thenReturn(sections);

        // Act
        List<SectionOutput> outputs = getSectionsUseCase.execute(1L);

        // Assert
        assertEquals(2, outputs.size());
        assertEquals("Chương 1", outputs.get(0).title());
        assertEquals(1, outputs.get(0).orderIndex());
        assertEquals("Chương 2", outputs.get(1).title());
        verify(sectionRepository).findByCourseId(1L);
    }

    @Test
    void execute_ShouldReturnEmptyList_WhenCourseHasNoSections() {
        // Arrange
        Course course = Course.reconstitute(1L, "Java 101", "Desc", 50, 0, BigDecimal.ZERO, 100L, true, true, null, null, java.util.List.of());
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(sectionRepository.findByCourseId(1L)).thenReturn(new ArrayList<>());

        // Act
        List<SectionOutput> outputs = getSectionsUseCase.execute(1L);

        // Assert
        assertTrue(outputs.isEmpty());
    }

    @Test
    void execute_ShouldThrowCourseNotFoundException_WhenCourseDoesNotExist() {
        // Arrange
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CourseNotFoundException.class, () -> getSectionsUseCase.execute(99L));
        verify(sectionRepository, never()).findByCourseId(any());
    }
}
