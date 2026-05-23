package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.CourseEntity.CourseSectionJpaEntity;
import com.example.learning_system_spring.adapter.repository.jpa.CourseEntity.CourseJpaEntity;
import com.example.learning_system_spring.application.repository.Course.CourseSectionRepository;
import com.example.learning_system_spring.domain.model.CourseSection;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CourseSectionRepositoryImpl implements CourseSectionRepository {

    private final JpaCourseSectionRepository jpaRepo;
    private final EntityManager em;

    @Override
    public Optional<CourseSection> findById(Long id) {
        return jpaRepo.findById(id).map(CourseSectionJpaEntity::toDomain);
    }

    @Override
    public List<CourseSection> findByCourseId(Long courseId) {
        return jpaRepo.findByCourseIdOrderByOrderIndex(courseId).stream()
                .map(CourseSectionJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public CourseSection save(CourseSection section, Long courseId) {
        CourseSectionJpaEntity entity = CourseSectionJpaEntity.fromDomain(section);

        // Gán course reference mà không cần load toàn bộ CourseJpaEntity
        CourseJpaEntity courseRef = em.getReference(CourseJpaEntity.class, courseId);
        entity.setCourse(courseRef);

        // Gán lại section reference cho từng lesson (để JPA biết FK)
        entity.getLessons().forEach(lesson -> lesson.setSection(entity));

        CourseSectionJpaEntity saved = jpaRepo.save(entity);
        return saved.toDomain();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepo.deleteById(id);
    }
}
