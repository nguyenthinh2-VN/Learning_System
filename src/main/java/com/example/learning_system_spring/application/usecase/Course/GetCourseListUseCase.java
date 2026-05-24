package com.example.learning_system_spring.application.usecase.Course;

import com.example.learning_system_spring.application.dto.Course.GetCourseListInput;
import com.example.learning_system_spring.application.dto.Course.GetCourseListOutput;
import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.domain.exception.CourseAccessDeniedException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.service.CourseOwnershipPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetCourseListUseCase {
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public PageResult<GetCourseListOutput> execute(GetCourseListInput input) {
        PageResult<Course> coursesPage;

        switch (input.scope()) {
            case PUBLIC -> coursesPage = courseRepository.searchPublishedCourses(input.keyword(), input.page(), input.size());
            case PENDING -> {
                requireAdmin(input);
                coursesPage = courseRepository.searchPendingCourses(input.keyword(), input.page(), input.size());
            }
            case ALL -> {
                requireAdmin(input);
                coursesPage = courseRepository.searchAllCourses(input.keyword(), input.page(), input.size());
            }
            case INSTRUCTOR -> {
                if (input.requesterId() == null) {
                    throw new CourseAccessDeniedException("Cần đăng nhập để xem danh sách khóa học của instructor.");
                }
                coursesPage = courseRepository.searchByInstructorId(input.requesterId(), input.keyword(), input.page(), input.size());
            }
            default -> throw new IllegalArgumentException("Unknown scope: " + input.scope());
        }

        List<GetCourseListOutput> items = coursesPage.items().stream()
                .map(GetCourseListOutput::from)
                .toList();

        return PageResult.of(
                coursesPage.totalElements(),
                coursesPage.totalPages(),
                coursesPage.page(),
                coursesPage.size(),
                items);
    }

    private void requireAdmin(GetCourseListInput input) {
        if (input.requesterRole() == null || !CourseOwnershipPolicy.hasFullCourseAccess(input.requesterRole())) {
            throw new CourseAccessDeniedException("Bạn không có quyền truy cập danh sách này.");
        }
    }
}
