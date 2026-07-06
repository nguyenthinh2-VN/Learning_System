package com.example.learning_system_spring.domain.model;

import java.time.LocalDateTime;

public class SectionTestProgress {
    private Long id;
    private Long userId;
    private Long sectionId;
    private Long courseId;
    private double score;
    private boolean passed;
    private LocalDateTime completedAt;

    private SectionTestProgress() {
    }

    public static SectionTestProgress create(Long userId, Long sectionId, Long courseId, double score, boolean passed) {
        if (userId == null || sectionId == null || courseId == null) {
            throw new IllegalArgumentException("userId, sectionId, courseId must not be null");
        }
        SectionTestProgress p = new SectionTestProgress();
        p.userId = userId;
        p.sectionId = sectionId;
        p.courseId = courseId;
        p.score = score;
        p.passed = passed;
        p.completedAt = LocalDateTime.now();
        return p;
    }

    public static SectionTestProgress reconstitute(Long id, Long userId, Long sectionId, Long courseId,
            double score, boolean passed, LocalDateTime completedAt) {
        SectionTestProgress p = new SectionTestProgress();
        p.id = id;
        p.userId = userId;
        p.sectionId = sectionId;
        p.courseId = courseId;
        p.score = score;
        p.passed = passed;
        p.completedAt = completedAt;
        return p;
    }

    public void updateScore(double score, boolean passed) {
        this.score = score;
        this.passed = passed;
        this.completedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public double getScore() {
        return score;
    }

    public boolean isPassed() {
        return passed;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
