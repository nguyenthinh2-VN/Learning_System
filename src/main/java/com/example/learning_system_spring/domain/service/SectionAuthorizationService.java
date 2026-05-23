package com.example.learning_system_spring.domain.service;

import com.example.learning_system_spring.domain.exception.SectionAccessDeniedException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.Role;
import org.springframework.stereotype.Service;

@Service
public class SectionAuthorizationService {

    /**
     * Kiểm tra quyền tạo section trong một course.
     * - INSTRUCTOR: chỉ được tạo section trong course của chính mình
     * - STAFF / SUPER_ADMIN: toàn quyền
     * - MEMBER / ADMIN_USER: không có quyền
     */
    public void authorizeCreate(Course course, Long requesterId, Role requesterRole) {
        authorize(course, requesterId, requesterRole, "thêm chương học");
    }

    /**
     * Kiểm tra quyền sửa / xóa section.
     */
    public void authorizeEditOrDelete(Course course, Long requesterId, Role requesterRole) {
        authorize(course, requesterId, requesterRole, "sửa/xóa chương học");
    }

    private void authorize(Course course, Long requesterId, Role requesterRole, String action) {
        if (requesterRole.isInstructor()) {
            if (!CourseOwnershipPolicy.isOwner(course, requesterId)) {
                throw new SectionAccessDeniedException(
                        "Giảng viên chỉ có quyền " + action + " trong khóa học do chính mình tạo.");
            }
        } else if (!CourseOwnershipPolicy.hasFullAccess(requesterRole)) {
            // ADMIN_USER và MEMBER đều bị từ chối
            throw new SectionAccessDeniedException("Bạn không có quyền " + action + ".");
        }
    }
}
