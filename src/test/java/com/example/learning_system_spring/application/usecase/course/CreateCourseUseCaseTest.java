package com.example.learning_system_spring.application.usecase.course;

import com.example.learning_system_spring.application.dto.Course.CourseOutput;
import com.example.learning_system_spring.application.dto.Course.CreateCourseInput;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.usecase.Course.CreateCourseUseCase;
import com.example.learning_system_spring.application.usecase.strategy.course.CourseManagementStrategy;
import com.example.learning_system_spring.application.usecase.strategy.course.CourseStrategyFactory;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CreateCourseUseCaseTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseStrategyFactory strategyFactory;

    @Mock
    private CourseManagementStrategy strategy;

    @InjectMocks
    private CreateCourseUseCase createCourseUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_WhenInstructorCreates_ShouldUseRequesterId() {
        // Arrange
        Role instructorRole = Role.reconstitute(2L, "INSTRUCTOR", "Giảng viên");
        CreateCourseInput input = new CreateCourseInput(
                100L, instructorRole, "Java 101", "Learn Java", 50, java.math.BigDecimal.ZERO, 999L, null);

        when(strategyFactory.getStrategy(instructorRole)).thenReturn(strategy);
        when(strategy.resolveInstructorId(100L, 999L)).thenReturn(100L); // Instructor ignores requestedInstructorId

        Course savedCourse = Course.reconstitute(1L, "Java 101", "Learn Java", 50, 0, java.math.BigDecimal.ZERO, 100L,
                null);
        when(courseRepository.save(any(Course.class))).thenReturn(savedCourse);

        // Act
        CourseOutput output = createCourseUseCase.execute(input);

        // Assert
        assertNotNull(output);
        assertEquals("Java 101", output.title());
        assertEquals(100L, output.instructorId());
    }

    @Test
    void execute_WhenStaffCreates_ShouldUseRequestedInstructorId() {
        // Arrange
        Role staffRole = Role.reconstitute(3L, "STAFF", "Nhân viên");
        CreateCourseInput input = new CreateCourseInput(
                200L, staffRole, "Spring Boot", "Learn Spring", 50, java.math.BigDecimal.ZERO, 999L, null);

        when(strategyFactory.getStrategy(staffRole)).thenReturn(strategy);
        when(strategy.resolveInstructorId(200L, 999L)).thenReturn(999L); // Staff uses requestedInstructorId

        Course savedCourse = Course.reconstitute(2L, "Spring Boot", "Learn Spring", 50, 0, java.math.BigDecimal.ZERO,
                999L, null);
        when(courseRepository.save(any(Course.class))).thenReturn(savedCourse);

        // Act
        CourseOutput output = createCourseUseCase.execute(input);

        // Assert
        assertNotNull(output);
        assertEquals("Spring Boot", output.title());
        assertEquals(999L, output.instructorId());
    }
}
