package com.example.learning_system_spring.adapter.repository.jpa;

import com.example.learning_system_spring.domain.model.Course;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int maxStudents;

    @Column(nullable = false)
    private int enrolledCount;

    public Course toDomain() {
        return Course.reconstitute(id, title, description, maxStudents, enrolledCount);
    }

    public static CourseJpaEntity fromDomain(Course course) {
        CourseJpaEntity entity = new CourseJpaEntity();
        entity.id = course.getId();
        entity.title = course.getTitle();
        entity.description = course.getDescription();
        entity.maxStudents = course.getMaxStudents();
        entity.enrolledCount = course.getEnrolledCount();
        return entity;
    }
}
