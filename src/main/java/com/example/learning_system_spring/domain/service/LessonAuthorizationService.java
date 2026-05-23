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
            return; // SUPER_ADMIN và STAFF có toàn quyền
        }
        
        if (requesterRole.isInstructor()) {
            // INSTRUCTOR chỉ được tạo lesson cho course của mình
            if (CourseOwnershipPolicy.isInstructorOwner(course, requesterId, requesterRole)) {
                return;
            }
            throw new LessonAccessDeniedException("Giảng viên không có quyền tạo bài giảng cho khóa học này");
        }
        
        // ADMIN_USER và MEMBER không có quyền
        throw new LessonAccessDeniedException("Không có quyền tạo bài giảng");
    }
    
    /**
     * Kiểm tra quyền xem lesson
     */
    public void authorizeView(Course course, Long requesterId, Role requesterRole) {
        // Tất cả role đều có quyền xem
        // Nếu cần kiểm tra enrollment sau này, thêm logic ở đây
    }
    
    /**
     * Kiểm tra quyền sửa/xóa lesson
     */
    public void authorizeEditOrDelete(Course course, Long requesterId, Role requesterRole) {
        if (requesterRole.isSuperAdmin() || requesterRole.isStaff()) {
            return; // SUPER_ADMIN và STAFF có toàn quyền
        }
        
        if (requesterRole.isInstructor()) {
            // INSTRUCTOR chỉ được sửa/xóa lesson cho course của mình
            if (CourseOwnershipPolicy.isInstructorOwner(course, requesterId, requesterRole)) {
                return;
            }
            throw new LessonAccessDeniedException("Giảng viên không có quyền thao tác bài giảng cho khóa học này");
        }
        
        // ADMIN_USER và MEMBER không có quyền
        throw new LessonAccessDeniedException("Không có quyền thao tác bài giảng");
    }
}