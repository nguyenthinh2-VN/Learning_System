package com.example.learning_system_spring.adapter.repository.jpa.CourseEntity;

import com.example.learning_system_spring.domain.model.CourseLesson;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course_lessons")
@Getter
@Setter
@NoArgsConstructor
public class CourseLessonJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "content_url")
    private String contentUrl;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private CourseSectionJpaEntity section;

    public CourseLesson toDomain() {
        return CourseLesson.reconstitute(id, title, contentUrl, orderIndex);
    }

    public static CourseLessonJpaEntity fromDomain(CourseLesson lesson) {
        CourseLessonJpaEntity entity = new CourseLessonJpaEntity();
        entity.id = lesson.getId();
        entity.title = lesson.getTitle();
        entity.contentUrl = lesson.getContentUrl();
        entity.orderIndex = lesson.getOrderIndex();
        return entity;
    }
}
