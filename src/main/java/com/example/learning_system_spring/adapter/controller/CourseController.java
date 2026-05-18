package com.example.learning_system_spring.adapter.controller;

import com.example.learning_system_spring.adapter.dto.response.CourseDetailResponse;
import com.example.learning_system_spring.adapter.dto.response.CourseListResponse;
import com.example.learning_system_spring.application.dto.GetCourseDetailInput;
import com.example.learning_system_spring.application.dto.GetCourseDetailOutput;
import com.example.learning_system_spring.application.dto.GetCourseListInput;
import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.application.usecase.GetCourseDetailUseCase;
import com.example.learning_system_spring.application.usecase.GetCourseListUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final GetCourseListUseCase getCourseListUseCase;
    private final GetCourseDetailUseCase getCourseDetailUseCase;

    @GetMapping
    public ResponseEntity<?> getCourses(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        GetCourseListInput input = new GetCourseListInput(keyword, page, size);
        var output = getCourseListUseCase.execute(input);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Success",
                "data", CourseListResponse.from(output),
                "timestamp", LocalDateTime.now()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCourseDetail(@PathVariable Long id) {
        GetCourseDetailInput input = new GetCourseDetailInput(id);
        GetCourseDetailOutput output = getCourseDetailUseCase.execute(input);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Success",
                "data", CourseDetailResponse.from(output),
                "timestamp", LocalDateTime.now()));
    }
}
