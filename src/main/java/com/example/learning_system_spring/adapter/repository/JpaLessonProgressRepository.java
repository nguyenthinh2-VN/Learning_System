package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.CourseEntity.LessonProgressJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface JpaLessonProgressRepository extends JpaRepository<LessonProgressJpaEntity, Long> {

    boolean existsByUserIdAndLessonId(Long userId, Long lessonId);

    void deleteByUserIdAndLessonId(Long userId, Long lessonId);

    long countByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT p.lessonId FROM LessonProgressJpaEntity p WHERE p.userId = :userId AND p.courseId = :courseId")
    List<Long> findCompletedLessonIds(@Param("userId") Long userId, @Param("courseId") Long courseId);
}
