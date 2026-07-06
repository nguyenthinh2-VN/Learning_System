package com.example.learning_system_spring.adapter.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
public class SubmitSectionTestRequest {
    // Map of questionId (or index) to selected answer (A, B, C, D)
    private Map<String, String> answers;
}
