package com.example.learning_system_spring.application.repository.Course;

import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.domain.model.Enrollment;

public interface EnrollmentRepository {
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);
    Enrollment save(Enrollment enrollment);
    PageResult<Enrollment> findByUserId(Long userId, int page, int size);
}
