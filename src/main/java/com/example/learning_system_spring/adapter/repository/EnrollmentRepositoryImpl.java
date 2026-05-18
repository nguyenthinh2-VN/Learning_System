package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.CourseEntity.CourseJpaEntity;
import com.example.learning_system_spring.adapter.repository.jpa.CourseEntity.EnrollmentJpaEntity;
import com.example.learning_system_spring.adapter.repository.jpa.UserEntity.UserJpaEntity;
import com.example.learning_system_spring.application.repository.Course.EnrollmentRepository;
import com.example.learning_system_spring.domain.model.Enrollment;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EnrollmentRepositoryImpl implements EnrollmentRepository {

    private final JpaEnrollmentRepository jpaRepo;
    private final EntityManager em;

    @Override
    public boolean existsByUserIdAndCourseId(Long userId, Long courseId) {
        return jpaRepo.existsByUserIdAndCourseId(userId, courseId);
    }

    @Override
    public Enrollment save(Enrollment enrollment) {
        EnrollmentJpaEntity entity = new EnrollmentJpaEntity();
        entity.setId(enrollment.getId());
        entity.setPaidPrice(enrollment.getPaidPrice());
        entity.setEnrolledAt(enrollment.getEnrolledAt());
        
        // Use getReference to avoid loading full entities just for foreign keys
        UserJpaEntity userRef = em.getReference(UserJpaEntity.class, enrollment.getUserId());
        CourseJpaEntity courseRef = em.getReference(CourseJpaEntity.class, enrollment.getCourseId());
        
        entity.setUser(userRef);
        entity.setCourse(courseRef);
        
        EnrollmentJpaEntity saved = jpaRepo.save(entity);
        return saved.toDomain();
    }
}
