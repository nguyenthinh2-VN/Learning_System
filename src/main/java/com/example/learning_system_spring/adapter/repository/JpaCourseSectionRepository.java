package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.CourseEntity.CourseSectionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface JpaCourseSectionRepository extends JpaRepository<CourseSectionJpaEntity, Long> {

    @Query("SELECT s FROM CourseSectionJpaEntity s LEFT JOIN FETCH s.lessons WHERE s.course.id = :courseId ORDER BY s.orderIndex ASC")
    List<CourseSectionJpaEntity> findByCourseIdOrderByOrderIndex(@Param("courseId") Long courseId);
}
