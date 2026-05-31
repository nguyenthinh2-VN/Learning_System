package com.example.learning_system_spring.domain.model;

import java.time.LocalDateTime;

/**
 * Bản ghi đánh dấu một lesson đã được một user hoàn thành.
 * POJO thuần — không annotation JPA.
 *
 * Lưu kèm courseId (denormalize) để đếm tiến độ theo (userId, courseId)
 * bằng một query, không phải join lesson → section → course.
 */
public class LessonProgress {

    private Long id;
    private Long userId;
    private Long lessonId;
    private Long courseId;
    private LocalDateTime completedAt;

    private LessonProgress() {}

    public static LessonProgress create(Long userId, Long lessonId, Long courseId) {
        if (userId == null || lessonId == null || courseId == null) {
            throw new IllegalArgumentException("userId, lessonId, courseId must not be null");
        }
        LessonProgress p = new LessonProgress();
        p.userId = userId;
        p.lessonId = lessonId;
        p.courseId = courseId;
        p.completedAt = LocalDateTime.now();
        return p;
    }

    public static LessonProgress reconstitute(Long id, Long userId, Long lessonId, Long courseId,
                                              LocalDateTime completedAt) {
        LessonProgress p = new LessonProgress();
        p.id = id;
        p.userId = userId;
        p.lessonId = lessonId;
        p.courseId = courseId;
        p.completedAt = completedAt;
        return p;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getLessonId() { return lessonId; }
    public Long getCourseId() { return courseId; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
