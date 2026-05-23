package com.example.learning_system_spring.application.repository.Course;

import com.example.learning_system_spring.domain.model.CourseSection;

import java.util.List;
import java.util.Optional;

public interface CourseSectionRepository {
    Optional<CourseSection> findById(Long id);
    List<CourseSection> findByCourseId(Long courseId);
    CourseSection save(CourseSection section, Long courseId);
    void deleteById(Long id);
}
