package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.CourseJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface JpaCourseRepository extends JpaRepository<CourseJpaEntity, Long> {
    
    @Query("SELECT c FROM CourseJpaEntity c WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<CourseJpaEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
