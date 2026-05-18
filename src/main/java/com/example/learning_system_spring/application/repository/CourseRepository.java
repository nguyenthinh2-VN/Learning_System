package com.example.learning_system_spring.application.repository;

import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.domain.model.Course;

import java.util.Optional;

public interface CourseRepository {
    PageResult<Course> searchCourses(String keyword, int page, int size);
    Optional<Course> findById(Long id);
}
