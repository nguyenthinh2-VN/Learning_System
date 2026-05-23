package com.example.learning_system_spring.adapter.repository.jpa.CourseEntity;

import com.example.learning_system_spring.domain.model.CourseLesson;
import com.example.learning_system_spring.domain.model.CourseSection;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "course_sections")
@Getter
@Setter
@NoArgsConstructor
public class CourseSectionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private CourseJpaEntity course;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseLessonJpaEntity> lessons = new ArrayList<>();

    public void addLesson(CourseLessonJpaEntity lesson) {
        lessons.add(lesson);
        lesson.setSection(this);
    }

    public void removeLesson(CourseLessonJpaEntity lesson) {
        lessons.remove(lesson);
        lesson.setSection(null);
    }

    public CourseSection toDomain() {
        List<CourseLesson> domainLessons = lessons.stream()
                .map(CourseLessonJpaEntity::toDomain)
                .collect(Collectors.toList());
        return CourseSection.reconstitute(id, title, orderIndex, domainLessons);
    }

    public static CourseSectionJpaEntity fromDomain(CourseSection section) {
        CourseSectionJpaEntity entity = new CourseSectionJpaEntity();
        entity.id = section.getId();
        entity.title = section.getTitle();
        entity.orderIndex = section.getOrderIndex();
        if (section.getLessons() != null) {
            section.getLessons().forEach(lesson -> {
                CourseLessonJpaEntity lessonEntity = CourseLessonJpaEntity.fromDomain(lesson);
                entity.addLesson(lessonEntity);
            });
        }
        return entity;
    }
}
