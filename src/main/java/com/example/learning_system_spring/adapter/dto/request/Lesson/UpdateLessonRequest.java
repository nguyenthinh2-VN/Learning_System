package com.example.learning_system_spring.adapter.dto.request.Lesson;

import com.example.learning_system_spring.application.dto.Lesson.UpdateLessonInput;
import com.example.learning_system_spring.domain.model.Role;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateLessonRequest(
    @NotBlank(message = "Tiêu đề bài giảng không được để trống")
    String title,
    
    @NotBlank(message = "URL nội dung không được để trống")
    String contentUrl,
    
    @Min(value = 0, message = "Thứ tự bài giảng phải lớn hơn hoặc bằng 0")
    int orderIndex
) {
    public UpdateLessonInput toInput(Long lessonId, Long courseId, Long sectionId, Long requesterId, Role requesterRole) {
        return new UpdateLessonInput(
            lessonId,
            courseId,
            sectionId,
            requesterId,
            requesterRole,
            title,
            contentUrl,
            orderIndex
        );
    }
}