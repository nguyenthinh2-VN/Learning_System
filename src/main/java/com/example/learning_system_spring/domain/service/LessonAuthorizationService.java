package com.example.learning_system_spring.domain.service;

import com.example.learning_system_spring.domain.exception.LessonAccessDeniedException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.Role;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LessonAuthorizationService {

    /**
     * Kiểm tra quyền tạo lesson
     */
    public void authorizeCreate(Course course, Long requesterId, Role requesterRole) {
        if (requesterRole.isSuperAdmin() || requesterRole.isStaff()) {
            return;
        }
        if (requesterRole.isInstructor()) {
            if (CourseOwnershipPolicy.isInstructorOwner(course, requesterId, requesterRole)) {
                return;
            }
            throw new LessonAccessDeniedException("Giảng viên không có quyền tạo bài giảng cho khóa học này");
        }
        throw new LessonAccessDeniedException("Không có quyền tạo bài giảng");
    }

    /**
     * Kiểm tra quyền xem lesson (có kiểm soát truy cập theo enrollment).
     *
     * | Role         | Quy tắc                                                  |
     * |--------------|----------------------------------------------------------|
     * | SUPER_ADMIN  | Luôn được phép                                           |
     * | STAFF        | Luôn được phép                                           |
     * | INSTRUCTOR   | Chỉ được phép nếu sở hữu khóa học                        |
     * | MEMBER       | Chỉ được phép nếu isEnrolled = true                      |
     * | ADMIN_USER   | Từ chối (không thuộc luồng học tập)                      |
     *
     * @param isEnrolled true nếu MEMBER đã có enrollment row cho course này
     */
    public void authorizeView(Course course, Long requesterId, Role requesterRole, boolean isEnrolled) {
        if (requesterRole.isSuperAdmin() || requesterRole.isStaff()) {
            return;
        }
        if (requesterRole.isInstructor()) {
            if (CourseOwnershipPolicy.isInstructorOwner(course, requesterId, requesterRole)) {
                return;
            }
            throw new LessonAccessDeniedException(
                    "Giảng viên chỉ có quyền xem bài giảng trong khóa học do chính mình tạo.");
        }
        if (requesterRole.isMember()) {
            if (isEnrolled) {
                return;
            }
            throw new LessonAccessDeniedException(
                    "Bạn chưa đăng ký khóa học này. Vui lòng mua khóa học để xem bài giảng.");
        }
        // ADMIN_USER và các role khác
        throw new LessonAccessDeniedException("Bạn không có quyền xem bài giảng.");
    }

    /**
     * Kiểm tra quyền sửa/xóa lesson
     */
    public void authorizeEditOrDelete(Course course, Long requesterId, Role requesterRole) {
        if (requesterRole.isSuperAdmin() || requesterRole.isStaff()) {
            return;
        }
        if (requesterRole.isInstructor()) {
            if (CourseOwnershipPolicy.isInstructorOwner(course, requesterId, requesterRole)) {
                return;
            }
            throw new LessonAccessDeniedException("Giảng viên không có quyền thao tác bài giảng cho khóa học này");
        }
        throw new LessonAccessDeniedException("Không có quyền thao tác bài giảng");
    }
}