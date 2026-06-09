package com.example.learning_system_spring.application.repository.Cart;

import com.example.learning_system_spring.domain.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserId(Long userId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.userId = :userId AND c.courseId = :courseId")
    void deleteByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.userId = :userId AND c.courseId IN :courseIds")
    void deleteByUserIdAndCourseIdIn(@Param("userId") Long userId, @Param("courseIds") List<Long> courseIds);

    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
