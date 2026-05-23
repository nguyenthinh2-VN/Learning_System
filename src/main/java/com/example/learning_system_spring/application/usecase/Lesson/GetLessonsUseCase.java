package com.example.learning_system_spring.application.usecase.Lesson;

import com.example.learning_system_spring.application.dto.Lesson.GetLessonsInput;
import com.example.learning_system_spring.application.dto.Lesson.GetLessonsOutput;
import com.example.learning_system_spring.application.dto.Section.LessonOutput;
import com.example.learning_system_spring.application.repository.Course.CourseLessonRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.CourseLesson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetLessonsUseCase {
    private final CourseRepository courseRepository;
    private final CourseLessonRepository lessonRepository;

    @Transactional(readOnly = true)
    public GetLessonsOutput execute(GetLessonsInput input) {
        // Kiểm tra course tồn tại
        Course course = courseRepository.findById(input.courseId())
                .orElseThrow(() -> new CourseNotFoundException(input.courseId()));

        // Lấy danh sách lessons
        List<CourseLesson> lessons = lessonRepository.findBySectionId(input.sectionId());
        List<LessonOutput> lessonOutputs = lessons.stream()
                .map(LessonOutput::from)
                .toList();

        return GetLessonsOutput.of(input.courseId(), input.sectionId(), lessonOutputs);
    }
}