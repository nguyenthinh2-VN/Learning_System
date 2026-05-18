package com.example.learning_system_spring.adapter.repository.jpa.CourseEntity;

import com.example.learning_system_spring.adapter.repository.jpa.UserEntity.UserJpaEntity;
import com.example.learning_system_spring.domain.model.Enrollment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments")
@Getter
@Setter
@NoArgsConstructor
public class EnrollmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private CourseJpaEntity course;

    @Column(name = "paid_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal paidPrice;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private LocalDateTime enrolledAt;

    @PrePersist
    void prePersist() {
        if (this.enrolledAt == null) {
            this.enrolledAt = LocalDateTime.now();
        }
    }

    public Enrollment toDomain() {
        return Enrollment.reconstitute(id, user.getId(), course.getId(), paidPrice, enrolledAt);
    }
}
