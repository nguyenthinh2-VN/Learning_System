package com.example.learning_system_spring.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LessonProgressTest {

    @Test
    void create_SetsFields() {
        LessonProgress p = LessonProgress.create(5L, 3L, 7L);
        assertEquals(5L, p.getUserId());
        assertEquals(3L, p.getLessonId());
        assertEquals(7L, p.getCourseId());
        assertNotNull(p.getCompletedAt());
    }

    @Test
    void create_RejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> LessonProgress.create(null, 3L, 7L));
        assertThrows(IllegalArgumentException.class, () -> LessonProgress.create(5L, null, 7L));
        assertThrows(IllegalArgumentException.class, () -> LessonProgress.create(5L, 3L, null));
    }
}
