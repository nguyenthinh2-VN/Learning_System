package com.example.learning_system_spring.adapter.repository.jpa.CourseEntity;

import com.example.learning_system_spring.domain.model.LessonProgress;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "lesson_progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_lesson_progress_user_lesson", columnNames = {"user_id", "lesson_id"})
        },
        indexes = {
                @Index(name = "idx_lesson_progress_user_course", columnList = "user_id, course_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class LessonProgressJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    public LessonProgress toDomain() {
        return LessonProgress.reconstitute(id, userId, lessonId, courseId, completedAt);
    }

    public static LessonProgressJpaEntity fromDomain(LessonProgress p) {
        LessonProgressJpaEntity e = new LessonProgressJpaEntity();
        e.id = p.getId();
        e.userId = p.getUserId();
        e.lessonId = p.getLessonId();
        e.courseId = p.getCourseId();
        e.completedAt = p.getCompletedAt();
        return e;
    }
}
