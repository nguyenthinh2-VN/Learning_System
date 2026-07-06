package com.example.learning_system_spring.application.usecase.Course;

import com.example.learning_system_spring.application.repository.Course.CourseSectionRepository;
import com.example.learning_system_spring.domain.exception.SectionTestNotFoundException;
import com.example.learning_system_spring.domain.exception.InvalidTestFormatException;
import com.example.learning_system_spring.domain.model.CourseSection;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetSectionTestUseCase {

    private final CourseSectionRepository sectionRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public List<Map<String, Object>> execute(Long sectionId) {
        CourseSection section = sectionRepo.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));

        if (section.getTestContent() == null || section.getTestContent().isBlank()) {
            throw new SectionTestNotFoundException("This section does not have a test");
        }

        try {
            List<Map<String, Object>> questions = objectMapper.readValue(section.getTestContent(),
                    new TypeReference<List<Map<String, Object>>>() {
                    });

            // Remove correctAnswer from output
            return questions.stream().map(q -> {
                q.remove("correctAnswer");
                return q;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            throw new InvalidTestFormatException("Invalid test format");
        }
    }
}
