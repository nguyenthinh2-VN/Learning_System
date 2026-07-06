package com.example.learning_system_spring.application.usecase.Course;

import com.example.learning_system_spring.adapter.dto.request.SubmitSectionTestRequest;
import com.example.learning_system_spring.application.repository.Course.CourseSectionRepository;
import com.example.learning_system_spring.application.repository.Course.SectionTestProgressRepository;
import com.example.learning_system_spring.domain.model.CourseSection;
import com.example.learning_system_spring.domain.model.SectionTestProgress;
import com.example.learning_system_spring.domain.exception.SectionTestNotFoundException;
import com.example.learning_system_spring.domain.exception.InvalidTestFormatException;
import com.example.learning_system_spring.domain.exception.TestHasNoQuestionsException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubmitSectionTestUseCase {

    private final CourseSectionRepository sectionRepo;
    private final SectionTestProgressRepository progressRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public SubmitTestResult execute(Long sectionId, SubmitSectionTestRequest request, Long userId) {
        CourseSection section = sectionRepo.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));

        if (section.getTestContent() == null || section.getTestContent().isBlank()) {
            throw new SectionTestNotFoundException("This section does not have a test");
        }

        Long courseId = sectionRepo.findCourseIdBySectionId(sectionId);

        List<Map<String, Object>> questions;
        try {
            questions = objectMapper.readValue(section.getTestContent(),
                    new TypeReference<List<Map<String, Object>>>() {
                    });
        } catch (Exception e) {
            throw new InvalidTestFormatException("Invalid test format");
        }

        if (questions.isEmpty()) {
            throw new TestHasNoQuestionsException("Test has no questions");
        }

        int correctCount = 0;
        int totalQuestions = questions.size();

        for (int i = 0; i < totalQuestions; i++) {
            Map<String, Object> question = questions.get(i);
            String questionId = String.valueOf(i); // We'll just use the array index as questionId

            // If the JSON structure uses specific IDs, we could parse that. We'll stick to
            // index-based for simplicity.
            if (question.containsKey("id")) {
                questionId = String.valueOf(question.get("id"));
            }

            String expectedAnswer = String.valueOf(question.get("correctAnswer"));
            String actualAnswer = request.getAnswers().get(questionId);

            if (expectedAnswer.equals(actualAnswer)) {
                correctCount++;
            }
        }

        double score = (double) correctCount / totalQuestions * 100.0;
        boolean passed = score >= 50.0;

        Optional<SectionTestProgress> existing = progressRepo.findByUserIdAndSectionId(userId, sectionId);
        if (existing.isPresent()) {
            SectionTestProgress progress = existing.get();
            // Optional: Only update if the new score is better, or always update. We'll
            // always update for now.
            progress.updateScore(score, passed);
            progressRepo.save(progress);
        } else {
            SectionTestProgress progress = SectionTestProgress.create(userId, sectionId, courseId, score, passed);
            progressRepo.save(progress);
        }

        return new SubmitTestResult(score, passed, correctCount, totalQuestions);
    }

    public record SubmitTestResult(double score, boolean passed, int correctCount, int totalQuestions) {
    }
}
