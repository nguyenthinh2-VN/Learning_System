package com.example.learning_system_spring.adapter.repository.jpa.CourseEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaSectionTestProgressRepository extends JpaRepository<SectionTestProgressJpaEntity, Long> {
    boolean existsByUserIdAndSectionId(Long userId, Long sectionId);
    SectionTestProgressJpaEntity findByUserIdAndSectionId(Long userId, Long sectionId);
    List<SectionTestProgressJpaEntity> findByUserIdAndCourseId(Long userId, Long courseId);
    long countByUserIdAndCourseIdAndPassedTrue(Long userId, Long courseId);
}
