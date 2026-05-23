package com.example.learning_system_spring.domain.service;

import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.Role;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CourseOwnershipPolicyTest {

    private Course courseOwnedBy(Long instructorId) {
        return Course.reconstitute(1L, "Test Course", "Desc", 50, 0, BigDecimal.ZERO, instructorId, null);
    }

    // --- isOwner ---

    @Test
    void isOwner_ShouldReturnTrue_WhenInstructorIdMatches() {
        Course course = courseOwnedBy(100L);
        assertTrue(CourseOwnershipPolicy.isOwner(course, 100L));
    }

    @Test
    void isOwner_ShouldReturnFalse_WhenInstructorIdDiffers() {
        Course course = courseOwnedBy(100L);
        assertFalse(CourseOwnershipPolicy.isOwner(course, 999L));
    }

    @Test
    void isOwner_ShouldReturnFalse_WhenInstructorIdIsNull() {
        Course course = courseOwnedBy(null);
        assertFalse(CourseOwnershipPolicy.isOwner(course, 100L));
    }

    // --- hasFullAccess (STAFF / SUPER_ADMIN) ---

    @Test
    void hasFullAccess_ShouldReturnTrue_ForStaff() {
        Role staff = Role.reconstitute(3L, "STAFF", null);
        assertTrue(CourseOwnershipPolicy.hasFullAccess(staff));
    }

    @Test
    void hasFullAccess_ShouldReturnTrue_ForSuperAdmin() {
        Role superAdmin = Role.reconstitute(5L, "SUPER_ADMIN", null);
        assertTrue(CourseOwnershipPolicy.hasFullAccess(superAdmin));
    }

    @Test
    void hasFullAccess_ShouldReturnFalse_ForAdminUser() {
        Role adminUser = Role.reconstitute(4L, "ADMIN_USER", null);
        assertFalse(CourseOwnershipPolicy.hasFullAccess(adminUser));
    }

    @Test
    void hasFullAccess_ShouldReturnFalse_ForMember() {
        Role member = Role.reconstitute(1L, "MEMBER", null);
        assertFalse(CourseOwnershipPolicy.hasFullAccess(member));
    }

    // --- hasFullCourseAccess (STAFF / ADMIN_USER / SUPER_ADMIN) ---

    @Test
    void hasFullCourseAccess_ShouldReturnTrue_ForAdminUser() {
        Role adminUser = Role.reconstitute(4L, "ADMIN_USER", null);
        assertTrue(CourseOwnershipPolicy.hasFullCourseAccess(adminUser));
    }

    @Test
    void hasFullCourseAccess_ShouldReturnTrue_ForStaff() {
        Role staff = Role.reconstitute(3L, "STAFF", null);
        assertTrue(CourseOwnershipPolicy.hasFullCourseAccess(staff));
    }

    @Test
    void hasFullCourseAccess_ShouldReturnFalse_ForMember() {
        Role member = Role.reconstitute(1L, "MEMBER", null);
        assertFalse(CourseOwnershipPolicy.hasFullCourseAccess(member));
    }

    // --- isInstructorOwner ---

    @Test
    void isInstructorOwner_ShouldReturnTrue_WhenInstructorOwnsTheCourse() {
        Course course = courseOwnedBy(100L);
        Role instructor = Role.reconstitute(2L, "INSTRUCTOR", null);
        assertTrue(CourseOwnershipPolicy.isInstructorOwner(course, 100L, instructor));
    }

    @Test
    void isInstructorOwner_ShouldReturnFalse_WhenInstructorDoesNotOwnCourse() {
        Course course = courseOwnedBy(100L);
        Role instructor = Role.reconstitute(2L, "INSTRUCTOR", null);
        assertFalse(CourseOwnershipPolicy.isInstructorOwner(course, 999L, instructor));
    }

    @Test
    void isInstructorOwner_ShouldReturnFalse_WhenRoleIsNotInstructor() {
        Course course = courseOwnedBy(100L);
        Role staff = Role.reconstitute(3L, "STAFF", null);
        assertFalse(CourseOwnershipPolicy.isInstructorOwner(course, 100L, staff));
    }
}
