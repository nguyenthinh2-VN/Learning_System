package com.example.learning_system_spring.application.repository.Course;

import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.domain.model.Course;

import java.util.Optional;

public interface CourseRepository {

    /**
     * Tìm course PUBLIC (published = true) — dùng cho /api/v1/courses (member, guest).
     */
    PageResult<Course> searchPublishedCourses(String keyword, int page, int size);

    /**
     * Tìm course pending (published = false) — dùng cho /api/v1/admin/courses/pending.
     */
    PageResult<Course> searchPendingCourses(String keyword, int page, int size);

    /**
     * Tìm course toàn bộ (cả published và pending) — dùng cho /api/v1/admin/courses.
     */
    PageResult<Course> searchAllCourses(String keyword, int page, int size);

    /**
     * Tìm course theo instructor (cả pending và published) — dùng cho /api/v1/instructor/courses.
     */
    PageResult<Course> searchByInstructorId(Long instructorId, String keyword, int page, int size);

    Optional<Course> findById(Long id);
    Optional<Course> findByIdForUpdate(Long id);
    Course save(Course course);
    void deleteById(Long id);
}
