package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.CourseEntity.EnrollmentJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaEnrollmentRepository extends JpaRepository<EnrollmentJpaEntity, Long> {
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);
    Page<EnrollmentJpaEntity> findByUserId(Long userId, Pageable pageable);
}
