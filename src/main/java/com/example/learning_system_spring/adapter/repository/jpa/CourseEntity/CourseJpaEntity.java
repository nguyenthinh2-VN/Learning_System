package com.example.learning_system_spring.adapter.repository.jpa.CourseEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(
        name = "courses",
        indexes = {
                @Index(name = "idx_course_published", columnList = "published")
        }
)
@Getter
@Setter
@NoArgsConstructor
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

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "instructor_id")
    private Long instructorId;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean published = false;

    @Column(name = "price_locked", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean priceLocked = false;

    @Column(name = "free_for_internal", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean freeForInternal = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "published_by")
    private Long publishedBy;

    @Column(name = "is_mandatory", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isMandatory = false;

    @Column(name = "assigned_department_id")
    private Long assignedDepartmentId;

    @Column(name = "mandatory_deadline")
    private LocalDateTime mandatoryDeadline;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseSectionJpaEntity> sections = new ArrayList<>();

    public void addSection(CourseSectionJpaEntity section) {
        sections.add(section);
        section.setCourse(this);
    }

    public void removeSection(CourseSectionJpaEntity section) {
        sections.remove(section);
        section.setCourse(null);
    }
}
