package com.example.learning_system_spring.application.usecase.Progress;

import com.example.learning_system_spring.application.dto.Progress.ProgressDataOutput;
import com.example.learning_system_spring.application.repository.Course.CourseLessonRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.repository.Course.EnrollmentRepository;
import com.example.learning_system_spring.application.repository.Course.LessonProgressRepository;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.Enrollment;
import com.example.learning_system_spring.domain.model.LessonProgress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetProgressUseCase {

    private final LessonProgressRepository lessonProgressRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final CourseLessonRepository courseLessonRepository;

    @Transactional(readOnly = true)
    public ProgressDataOutput execute(Long userId) {
        List<LessonProgress> progresses = lessonProgressRepository.findByUserId(userId);
        List<Enrollment> enrollments = enrollmentRepository.findAllByUserId(userId);

        // 1. Calculate Heatmap
        Map<LocalDate, Long> progressByDate = progresses.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCompletedAt().toLocalDate(),
                        Collectors.counting()));

        List<ProgressDataOutput.HeatmapDataDTO> heatmap = progressByDate.entrySet().stream()
                .map(entry -> {
                    int count = entry.getValue().intValue();
                    int level = calculateHeatmapLevel(count);
                    return ProgressDataOutput.HeatmapDataDTO.builder()
                            .date(entry.getKey().format(DateTimeFormatter.ISO_LOCAL_DATE))
                            .count(count)
                            .level(level)
                            .build();
                })
                .sorted(Comparator.comparing(ProgressDataOutput.HeatmapDataDTO::getDate))
                .collect(Collectors.toList());

        // 2. Calculate Streak
        int streakDays = calculateStreak(progressByDate.keySet());

        // 3. Process Courses
        List<Long> courseIds = enrollments.stream().map(Enrollment::getCourseId).toList();
        List<Course> courses = courseIds.isEmpty() ? List.of() : courseRepository.findByIdIn(courseIds);
        Map<Long, Course> courseMap = courses.stream().collect(Collectors.toMap(Course::getId, c -> c));

        int completedCourses = 0;
        List<ProgressDataOutput.OngoingCourseDTO> ongoingCourses = new ArrayList<>();

        for (Enrollment enrollment : enrollments) {
            Course course = courseMap.get(enrollment.getCourseId());
            if (course == null)
                continue;

            long totalLessons = courseLessonRepository.countByCourseId(course.getId());
            long completedLessons = progresses.stream()
                    .filter(p -> p.getCourseId().equals(course.getId()))
                    .count();

            int progressPercentage = totalLessons > 0 ? (int) ((completedLessons * 100) / totalLessons) : 0;

            if (progressPercentage >= 100 && totalLessons > 0) {
                completedCourses++;
            }

            LocalDateTime lastAccessed = progresses.stream()
                    .filter(p -> p.getCourseId().equals(course.getId()))
                    .map(LessonProgress::getCompletedAt)
                    .max(LocalDateTime::compareTo)
                    .orElse(enrollment.getEnrolledAt());

            ongoingCourses.add(ProgressDataOutput.OngoingCourseDTO.builder()
                    .id(course.getId())
                    .title(course.getTitle())
                    .thumbnailUrl(course.getThumbnailUrl())
                    .progressPercentage(progressPercentage)
                    .lastAccessed(lastAccessed)
                    .build());
        }

        // Sort ongoing courses by last accessed
        ongoingCourses.sort((a, b) -> b.getLastAccessed().compareTo(a.getLastAccessed()));

        // 4. Calculate total hours
        double totalHours = progresses.size() * 0.5;

        ProgressDataOutput.OverviewDTO overview = ProgressDataOutput.OverviewDTO.builder()
                .totalHours(totalHours)
                .completedCourses(completedCourses)
                .streakDays(streakDays)
                .build();

        return ProgressDataOutput.builder()
                .overview(overview)
                .heatmap(heatmap)
                .ongoingCourses(ongoingCourses)
                .build();
    }

    private int calculateHeatmapLevel(int count) {
        if (count == 0)
            return 0;
        if (count <= 2)
            return 1;
        if (count <= 4)
            return 2;
        if (count <= 6)
            return 3;
        return 4;
    }

    private int calculateStreak(Set<LocalDate> dates) {
        if (dates.isEmpty())
            return 0;

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        List<LocalDate> sortedDates = dates.stream()
                .sorted(Comparator.reverseOrder())
                .toList();

        int streak = 0;
        LocalDate currentStreakDate = sortedDates.get(0);

        // Streak must start today or yesterday
        if (!currentStreakDate.equals(today) && !currentStreakDate.equals(yesterday)) {
            return 0;
        }

        LocalDate expectedDate = currentStreakDate;
        for (LocalDate date : sortedDates) {
            if (date.equals(expectedDate)) {
                streak++;
                expectedDate = expectedDate.minusDays(1);
            } else {
                break;
            }
        }

        return streak;
    }
}
