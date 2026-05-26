package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.CourseEntity.CourseJpaEntity;
import com.example.learning_system_spring.adapter.repository.jpa.CourseEntity.EnrollmentJpaEntity;
import com.example.learning_system_spring.adapter.repository.jpa.UserEntity.UserJpaEntity;
import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.application.repository.Course.EnrollmentRepository;
import com.example.learning_system_spring.domain.model.Enrollment;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

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

        UserJpaEntity userRef = em.getReference(UserJpaEntity.class, enrollment.getUserId());
        CourseJpaEntity courseRef = em.getReference(CourseJpaEntity.class, enrollment.getCourseId());

        entity.setUser(userRef);
        entity.setCourse(courseRef);

        EnrollmentJpaEntity saved = jpaRepo.save(entity);
        return saved.toDomain();
    }

    @Override
    public PageResult<Enrollment> findByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "enrolledAt"));
        Page<EnrollmentJpaEntity> jpaPage = jpaRepo.findByUserId(userId, pageable);

        List<Enrollment> items = jpaPage.getContent().stream()
                .map(EnrollmentJpaEntity::toDomain)
                .toList();

        return PageResult.of(
                jpaPage.getTotalElements(),
                jpaPage.getTotalPages(),
                page,
                size,
                items
        );
    }
}
