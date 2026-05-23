package com.example.learning_system_spring.domain.service;

import com.example.learning_system_spring.domain.exception.CourseAccessDeniedException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.Role;
import org.springframework.stereotype.Service;

@Service
public class CourseAuthorizationService {

    /**
     * Kiểm tra quyền sửa / xóa Course.
     * - INSTRUCTOR: chỉ được thao tác course của chính mình
     * - STAFF / ADMIN_USER / SUPER_ADMIN: toàn quyền
     * - MEMBER: không có quyền
     */
    public void authorizeEditOrDelete(Course course, Long requesterId, Role requesterRole) {
        if (requesterRole.isInstructor()) {
            if (!CourseOwnershipPolicy.isOwner(course, requesterId)) {
                throw new CourseAccessDeniedException("Giảng viên chỉ có quyền sửa/xóa khóa học do chính mình tạo.");
            }
        } else if (!CourseOwnershipPolicy.hasFullCourseAccess(requesterRole)) {
            throw new CourseAccessDeniedException("Bạn không có quyền sửa/xóa khóa học này.");
        }
    }
}
