package com.example.learning_system_spring.domain.service;

import com.example.learning_system_spring.domain.exception.CourseAccessDeniedException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.Role;
import org.springframework.stereotype.Service;

@Service
public class CourseAuthorizationService {

    public void authorizeEditOrDelete(Course course, Long userId, Role userRole) {
        if (userRole.isInstructor()) {
            if (course.getInstructorId() == null || !course.getInstructorId().equals(userId)) {
                throw new CourseAccessDeniedException("Giảng viên chỉ có quyền sửa/xóa khóa học do chính mình tạo.");
            }
        } else if (!userRole.isStaff() && !userRole.isAdminUser() && !userRole.isSuperAdmin()) {
            throw new CourseAccessDeniedException("Bạn không có quyền sửa/xóa khóa học này.");
        }
    }
}
