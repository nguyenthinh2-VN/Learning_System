package com.example.learning_system_spring.application.repository.Course;

import com.example.learning_system_spring.domain.model.CourseLesson;
import java.util.List;
import java.util.Optional;

public interface CourseLessonRepository {
    Optional<CourseLesson> findById(Long id);
    List<CourseLesson> findBySectionId(Long sectionId);
    CourseLesson save(CourseLesson lesson, Long sectionId);
    void deleteById(Long id);
    boolean existsBySectionIdAndOrderIndex(Long sectionId, int orderIndex);

    /** Tổng số lesson của một course (join lesson → section → course). */
    long countByCourseId(Long courseId);

    /** Lesson có thuộc course này không (chống thao tác lesson của khóa khác). */
    boolean existsByIdAndCourseId(Long lessonId, Long courseId);
}