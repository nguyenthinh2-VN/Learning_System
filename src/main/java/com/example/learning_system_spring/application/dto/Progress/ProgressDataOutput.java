package com.example.learning_system_spring.application.dto.Progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressDataOutput {
    private OverviewDTO overview;
    private List<HeatmapDataDTO> heatmap;
    private List<OngoingCourseDTO> ongoingCourses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverviewDTO {
        private double totalHours;
        private int completedCourses;
        private int streakDays;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatmapDataDTO {
        private String date; // YYYY-MM-DD
        private int count;
        private int level; // 0-4 for Github heatmap color intensity
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OngoingCourseDTO {
        private Long id;
        private String title;
        private String thumbnailUrl;
        private int progressPercentage;
        private LocalDateTime lastAccessed;
    }
}
