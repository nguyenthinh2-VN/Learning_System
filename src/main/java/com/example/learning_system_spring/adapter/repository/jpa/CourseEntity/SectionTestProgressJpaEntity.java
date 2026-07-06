package com.example.learning_system_spring.adapter.repository.jpa.CourseEntity;

import com.example.learning_system_spring.domain.model.SectionTestProgress;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "section_test_progress", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "section_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class SectionTestProgressJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "score", nullable = false)
    private double score;

    @Column(name = "passed", nullable = false)
    private boolean passed;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    public SectionTestProgress toDomain() {
        return SectionTestProgress.reconstitute(id, userId, sectionId, courseId, score, passed, completedAt);
    }

    public static SectionTestProgressJpaEntity fromDomain(SectionTestProgress domain) {
        SectionTestProgressJpaEntity entity = new SectionTestProgressJpaEntity();
        entity.id = domain.getId();
        entity.userId = domain.getUserId();
        entity.sectionId = domain.getSectionId();
        entity.courseId = domain.getCourseId();
        entity.score = domain.getScore();
        entity.passed = domain.isPassed();
        entity.completedAt = domain.getCompletedAt();
        return entity;
    }
}
