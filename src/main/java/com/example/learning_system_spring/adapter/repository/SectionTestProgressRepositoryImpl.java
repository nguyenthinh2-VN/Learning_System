package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.CourseEntity.JpaSectionTestProgressRepository;
import com.example.learning_system_spring.adapter.repository.jpa.CourseEntity.SectionTestProgressJpaEntity;
import com.example.learning_system_spring.application.repository.Course.SectionTestProgressRepository;
import com.example.learning_system_spring.domain.model.SectionTestProgress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SectionTestProgressRepositoryImpl implements SectionTestProgressRepository {

    private final JpaSectionTestProgressRepository jpaRepo;

    @Override
    public boolean existsByUserIdAndSectionId(Long userId, Long sectionId) {
        return jpaRepo.existsByUserIdAndSectionId(userId, sectionId);
    }

    @Override
    public Optional<SectionTestProgress> findByUserIdAndSectionId(Long userId, Long sectionId) {
        SectionTestProgressJpaEntity entity = jpaRepo.findByUserIdAndSectionId(userId, sectionId);
        return Optional.ofNullable(entity).map(SectionTestProgressJpaEntity::toDomain);
    }

    @Override
    public void save(SectionTestProgress progress) {
        jpaRepo.save(SectionTestProgressJpaEntity.fromDomain(progress));
    }

    @Override
    public List<SectionTestProgress> findByUserIdAndCourseId(Long userId, Long courseId) {
        return jpaRepo.findByUserIdAndCourseId(userId, courseId).stream()
                .map(SectionTestProgressJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countPassedByUserIdAndCourseId(Long userId, Long courseId) {
        return jpaRepo.countByUserIdAndCourseIdAndPassedTrue(userId, courseId);
    }
}
