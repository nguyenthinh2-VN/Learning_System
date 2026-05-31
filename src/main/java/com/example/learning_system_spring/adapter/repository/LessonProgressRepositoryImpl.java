package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.CourseEntity.LessonProgressJpaEntity;
import com.example.learning_system_spring.application.repository.Course.LessonProgressRepository;
import com.example.learning_system_spring.domain.model.LessonProgress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class LessonProgressRepositoryImpl implements LessonProgressRepository {

    private final JpaLessonProgressRepository jpaRepo;

    @Override
    public boolean existsByUserIdAndLessonId(Long userId, Long lessonId) {
        return jpaRepo.existsByUserIdAndLessonId(userId, lessonId);
    }

    @Override
    public void markComplete(LessonProgress progress) {
        // Use case đã kiểm tra tồn tại; UNIQUE (user_id, lesson_id) là phòng tuyến cuối.
        jpaRepo.save(LessonProgressJpaEntity.fromDomain(progress));
    }

    @Override
    public void unmarkComplete(Long userId, Long lessonId) {
        jpaRepo.deleteByUserIdAndLessonId(userId, lessonId);
    }

    @Override
    public long countByUserIdAndCourseId(Long userId, Long courseId) {
        return jpaRepo.countByUserIdAndCourseId(userId, courseId);
    }

    @Override
    public List<Long> findCompletedLessonIds(Long userId, Long courseId) {
        return jpaRepo.findCompletedLessonIds(userId, courseId);
    }
}
