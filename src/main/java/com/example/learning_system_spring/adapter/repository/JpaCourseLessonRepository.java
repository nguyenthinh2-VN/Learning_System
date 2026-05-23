package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.CourseEntity.CourseLessonJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
interface JpaCourseLessonRepository extends JpaRepository<CourseLessonJpaEntity, Long> {
    
    @Query("SELECT l FROM CourseLessonJpaEntity l WHERE l.section.id = :sectionId ORDER BY l.orderIndex")
    List<CourseLessonJpaEntity> findBySectionIdOrderByOrderIndex(@Param("sectionId") Long sectionId);
    
    @Query("SELECT COUNT(l) > 0 FROM CourseLessonJpaEntity l WHERE l.section.id = :sectionId AND l.orderIndex = :orderIndex")
    boolean existsBySectionIdAndOrderIndex(@Param("sectionId") Long sectionId, @Param("orderIndex") int orderIndex);
}