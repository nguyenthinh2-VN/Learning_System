package com.example.learning_system_spring.application.usecase.strategy.course;

import com.example.learning_system_spring.domain.exception.CourseAccessDeniedException;
import com.example.learning_system_spring.domain.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CourseStrategyFactory {

    private final List<CourseManagementStrategy> strategies;

    public CourseManagementStrategy getStrategy(Role userRole) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(userRole))
                .findFirst()
                .orElseThrow(() -> new CourseAccessDeniedException("Role của bạn không có quyền tạo khóa học."));
    }
}
