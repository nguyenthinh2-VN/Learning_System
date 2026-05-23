package com.example.learning_system_spring.domain.service;

import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.Role;

/**
 * Policy thuần túy kiểm tra quyền sở hữu / thao tác trên Course.
 * Không phụ thuộc framework, không ném exception — chỉ trả về boolean.
 * Các Service gọi vào đây để tái sử dụng logic thay vì lặp code.
 */
public class CourseOwnershipPolicy {

    private CourseOwnershipPolicy() {}

    /**
     * Kiểm tra user có phải chủ sở hữu course không.
     * (instructorId của course == requesterId)
     */
    public static boolean isOwner(Course course, Long requesterId) {
        return course.getInstructorId() != null && course.getInstructorId().equals(requesterId);
    }

    /**
     * Kiểm tra role có quyền toàn quyền (STAFF hoặc SUPER_ADMIN) không.
     */
    public static boolean hasFullAccess(Role role) {
        return role.isStaff() || role.isSuperAdmin();
    }

    /**
     * Kiểm tra role có quyền toàn quyền bao gồm cả ADMIN_USER không.
     * Dùng cho Course-level (ADMIN_USER được sửa/xóa course nhưng không được sửa section).
     */
    public static boolean hasFullCourseAccess(Role role) {
        return role.isStaff() || role.isAdminUser() || role.isSuperAdmin();
    }

    /**
     * Kiểm tra INSTRUCTOR có được thao tác trên course này không.
     * Điều kiện: phải là instructor VÀ phải là chủ sở hữu course.
     */
    public static boolean isInstructorOwner(Course course, Long requesterId, Role role) {
        return role.isInstructor() && isOwner(course, requesterId);
    }
}
