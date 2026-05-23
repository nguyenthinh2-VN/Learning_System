package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.CourseEntity.CourseLessonJpaEntity;
import com.example.learning_system_spring.adapter.repository.jpa.CourseEntity.CourseSectionJpaEntity;
import com.example.learning_system_spring.application.repository.Course.CourseLessonRepository;
import com.example.learning_system_spring.domain.exception.SectionNotFoundException;
import com.example.learning_system_spring.domain.model.CourseLesson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CourseLessonRepositoryImpl implements CourseLessonRepository {
    private final JpaCourseLessonRepository jpaLessonRepository;
    private final JpaCourseSectionRepository jpaSectionRepository;

    @Override
    public Optional<CourseLesson> findById(Long id) {
        return jpaLessonRepository.findById(id)
                .map(CourseLessonJpaEntity::toDomain);
    }

    @Override
    public List<CourseLesson> findBySectionId(Long sectionId) {
        return jpaLessonRepository.findBySectionIdOrderByOrderIndex(sectionId).stream()
                .map(CourseLessonJpaEntity::toDomain)
                .toList();
    }

    @Override
    public CourseLesson save(CourseLesson lesson, Long sectionId) {
        // Tìm section
        CourseSectionJpaEntity section = jpaSectionRepository.findById(sectionId)
                .orElseThrow(() -> new SectionNotFoundException(sectionId));

        // Chuyển đổi từ domain sang JPA entity
        CourseLessonJpaEntity lessonEntity = CourseLessonJpaEntity.fromDomain(lesson);
        lessonEntity.setSection(section);

        // Lưu và chuyển đổi ngược lại
        return jpaLessonRepository.save(lessonEntity).toDomain();
    }

    @Override
    public void deleteById(Long id) {
        jpaLessonRepository.deleteById(id);
    }

    @Override
    public boolean existsBySectionIdAndOrderIndex(Long sectionId, int orderIndex) {
        return jpaLessonRepository.existsBySectionIdAndOrderIndex(sectionId, orderIndex);
    }
}