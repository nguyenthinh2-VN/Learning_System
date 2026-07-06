package com.example.learning_system_spring.application.repository.Course;

import com.example.learning_system_spring.domain.model.SectionTestProgress;

import java.util.List;
import java.util.Optional;

public interface SectionTestProgressRepository {
    boolean existsByUserIdAndSectionId(Long userId, Long sectionId);

    Optional<SectionTestProgress> findByUserIdAndSectionId(Long userId, Long sectionId);

    void save(SectionTestProgress progress);

    List<SectionTestProgress> findByUserIdAndCourseId(Long userId, Long courseId);

    long countPassedByUserIdAndCourseId(Long userId, Long courseId);
}
