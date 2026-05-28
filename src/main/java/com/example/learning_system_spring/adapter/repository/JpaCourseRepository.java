package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.CourseEntity.CourseJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

interface JpaCourseRepository extends JpaRepository<CourseJpaEntity, Long> {

    @Query("SELECT c FROM CourseJpaEntity c "
            + "WHERE c.published = true "
            + "  AND (COALESCE(:keyword, '') = '' OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<CourseJpaEntity> searchPublished(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c FROM CourseJpaEntity c "
            + "WHERE c.published = false "
            + "  AND (COALESCE(:keyword, '') = '' OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<CourseJpaEntity> searchPending(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c FROM CourseJpaEntity c "
            + "WHERE COALESCE(:keyword, '') = '' OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<CourseJpaEntity> searchAll(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c FROM CourseJpaEntity c "
            + "WHERE c.instructorId = :instructorId "
            + "  AND (COALESCE(:keyword, '') = '' OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<CourseJpaEntity> searchByInstructorId(@Param("instructorId") Long instructorId,
                                               @Param("keyword") String keyword,
                                               Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CourseJpaEntity c WHERE c.id = :id")
    Optional<CourseJpaEntity> findByIdForUpdate(@Param("id") Long id);
}
