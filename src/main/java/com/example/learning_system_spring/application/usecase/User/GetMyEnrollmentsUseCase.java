package com.example.learning_system_spring.application.usecase.User;

import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.application.dto.User.MyEnrollmentOutput;
import com.example.learning_system_spring.application.repository.Course.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMyEnrollmentsUseCase {

    private final EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public PageResult<MyEnrollmentOutput> execute(Long requesterId, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page phải >= 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size phải trong khoảng [1, 100]");
        }

        return enrollmentRepository.findByUserId(requesterId, page, size)
                .map(MyEnrollmentOutput::from);
    }
}
