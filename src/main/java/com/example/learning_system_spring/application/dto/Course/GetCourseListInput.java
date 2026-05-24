package com.example.learning_system_spring.application.dto.Course;

import com.example.learning_system_spring.domain.model.Role;

/**
 * Input cho GetCourseListUseCase. Có scope để phân biệt:
 * - PUBLIC: chỉ trả về published = true (cho /api/v1/courses)
 * - PENDING: chỉ trả về published = false (cho /api/v1/admin/courses/pending)
 * - ALL: cả published và pending (cho /api/v1/admin/courses)
 * - INSTRUCTOR: chỉ course của instructor (cho /api/v1/instructor/courses)
 *
 * requesterId chỉ dùng khi scope = INSTRUCTOR.
 */
public record GetCourseListInput(
        String keyword,
        int page,
        int size,
        Scope scope,
        Long requesterId,
        Role requesterRole
) {
    public enum Scope { PUBLIC, PENDING, ALL, INSTRUCTOR }

    public static GetCourseListInput publicScope(String keyword, int page, int size) {
        return new GetCourseListInput(keyword, page, size, Scope.PUBLIC, null, null);
    }

    public static GetCourseListInput pendingScope(String keyword, int page, int size, Long requesterId, Role role) {
        return new GetCourseListInput(keyword, page, size, Scope.PENDING, requesterId, role);
    }

    public static GetCourseListInput allScope(String keyword, int page, int size, Long requesterId, Role role) {
        return new GetCourseListInput(keyword, page, size, Scope.ALL, requesterId, role);
    }

    public static GetCourseListInput instructorScope(String keyword, int page, int size, Long requesterId, Role role) {
        return new GetCourseListInput(keyword, page, size, Scope.INSTRUCTOR, requesterId, role);
    }
}
