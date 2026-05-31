package com.example.learning_system_spring.application.repository.Course;

import com.example.learning_system_spring.domain.model.LessonProgress;

import java.util.List;

public interface LessonProgressRepository {

    boolean existsByUserIdAndLessonId(Long userId, Long lessonId);

    /** Đánh dấu hoàn thành — idempotent (insert-if-absent). */
    void markComplete(LessonProgress progress);

    /** Bỏ đánh dấu — no-op nếu chưa có. */
    void unmarkComplete(Long userId, Long lessonId);

    /** Số lesson đã hoàn thành của user trong một course. */
    long countByUserIdAndCourseId(Long userId, Long courseId);

    /** Danh sách lessonId user đã hoàn thành trong một course. */
    List<Long> findCompletedLessonIds(Long userId, Long courseId);
}
