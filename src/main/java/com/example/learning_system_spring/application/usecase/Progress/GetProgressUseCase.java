package com.example.learning_system_spring.application.usecase.Progress;

import com.example.learning_system_spring.application.dto.Progress.ProgressDataOutput;
import com.example.learning_system_spring.application.repository.Course.CourseLessonRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.repository.Course.CourseSectionRepository;
import com.example.learning_system_spring.application.repository.Course.EnrollmentRepository;
import com.example.learning_system_spring.application.repository.Course.LessonProgressRepository;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.Enrollment;
import com.example.learning_system_spring.domain.model.LessonProgress;
import com.example.learning_system_spring.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.learning_system_spring.application.repository.Course.SectionTestProgressRepository;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.application.repository.Department.DepartmentRepository;

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
    private final CourseSectionRepository courseSectionRepository;
    private final CourseLessonRepository courseLessonRepository;
    private final SectionTestProgressRepository sectionTestProgressRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public ProgressDataOutput execute(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new com.example.learning_system_spring.domain.exception.UserNotFoundException(userId));
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
        Map<Long, Boolean> resultMap = new HashMap<>();

        for (Enrollment enrollment : enrollments) {
            Course course = courseMap.get(enrollment.getCourseId());
            if (course == null)
                continue;

            long totalLessons = courseLessonRepository.countByCourseId(course.getId());
            long completedLessons = progresses.stream()
                    .filter(p -> p.getCourseId().equals(course.getId()))
                    .count();

            int totalTests = (int) courseSectionRepository.countTotalTestsByCourseId(course.getId());
            int passedTests = (int) sectionTestProgressRepository.countPassedByUserIdAndCourseId(userId,
                    course.getId());

            int progressPercentage = totalLessons > 0 ? (int) ((completedLessons * 100) / totalLessons) : 0;

            if (progressPercentage >= 100 && totalLessons > 0) {
                completedCourses++;
            }

            LocalDateTime lastAccessed = progresses.stream()
                    .filter(p -> p.getCourseId().equals(course.getId()))
                    .map(LessonProgress::getCompletedAt)
                    .max(LocalDateTime::compareTo)
                    .orElse(enrollment.getEnrolledAt());

            boolean isMandatoryForUser = false;
            if (course.isMandatory() && course.getAssignedDepartmentId() != null && user.getDepartmentId() != null) {
                isMandatoryForUser = isDepartmentInHierarchy(user.getDepartmentId(), course.getAssignedDepartmentId());
            }
            String statusMessage = null;
            if (isMandatoryForUser && progressPercentage < 100) {
                statusMessage = "Chưa đạt kết quả do khóa bắt buộc";
            }

            ongoingCourses.add(ProgressDataOutput.OngoingCourseDTO.builder()
                    .id(course.getId())
                    .title(course.getTitle())
                    .thumbnailUrl(course.getThumbnailUrl())
                    .progressPercentage(progressPercentage)
                    .lastAccessed(lastAccessed)
                    .isMandatory(isMandatoryForUser)
                    .statusMessage(statusMessage)
                    .totalTests(totalTests)
                    .passedTests(passedTests)
                    .build());

            resultMap.put(course.getId(), true);
        }

        // Add mandatory courses not enrolled
        if (user.getDepartmentId() != null) {
            // Find all mandatory courses assigned to this department or its parents
            List<Long> hierarchyIds = getDepartmentHierarchy(user.getDepartmentId());
            List<Course> mandatoryCourses = courseRepository.findMandatoryCoursesByDepartmentIds(hierarchyIds);

            for (Course mCourse : mandatoryCourses) {
                // If it's already in the result, skip
                if (resultMap.containsKey(mCourse.getId())) {
                    continue;
                }
                ongoingCourses.add(ProgressDataOutput.OngoingCourseDTO.builder()
                        .id(mCourse.getId())
                        .title(mCourse.getTitle())
                        .thumbnailUrl(mCourse.getThumbnailUrl())
                        .progressPercentage(0)
                        .lastAccessed(LocalDateTime.now()) // Default cho lên đầu hoặc xử lý riêng
                        .isMandatory(true)
                        .statusMessage("Chưa được đăng ký học do khóa bắt buộc")
                        .totalTests(0)
                        .passedTests(0)
                        .build());
            }
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

        return ProgressDataOutput.builder().overview(overview).heatmap(heatmap).ongoingCourses(ongoingCourses).build();
    }

    // --- Helper methods for department hierarchy ---
    private boolean isDepartmentInHierarchy(Long userDepartmentId, Long courseDepartmentId) {
        if (userDepartmentId.equals(courseDepartmentId))
            return true;

        List<Long> hierarchy = getDepartmentHierarchy(userDepartmentId);
        return hierarchy.contains(courseDepartmentId);
    }

    private List<Long> getDepartmentHierarchy(Long departmentId) {
        java.util.List<Long> hierarchy = new java.util.ArrayList<>();
        Long currentId = departmentId;

        while (currentId != null) {
            hierarchy.add(currentId);
            currentId = departmentRepository.findById(currentId)
                    .map(com.example.learning_system_spring.domain.model.Department::getParentId)
                    .orElse(null);
        }
        return hierarchy;
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
