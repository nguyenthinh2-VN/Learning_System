package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.CourseEntity.CourseJpaEntity;
import com.example.learning_system_spring.adapter.mapper.CourseMapper;
import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.domain.model.Course;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CourseRepositoryImpl implements CourseRepository {

    private final JpaCourseRepository jpaCourseRepository;
    private final CourseMapper courseMapper;

    private String normalizeKeyword(String keyword) {
        // Trả về null nếu keyword rỗng — để COALESCE(:keyword, '') = '' hoạt động đúng
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return keyword.trim();
    }

    private PageResult<Course> toPageResult(Page<CourseJpaEntity> entityPage) {
        List<Course> courses = entityPage.getContent().stream()
                .map(courseMapper::toDomain)
                .toList();
        return PageResult.of(
                entityPage.getTotalElements(),
                entityPage.getTotalPages(),
                entityPage.getNumber(),
                entityPage.getSize(),
                courses);
    }

    @Override
    public PageResult<Course> searchPublishedCourses(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return toPageResult(jpaCourseRepository.searchPublished(normalizeKeyword(keyword), pageable));
    }

    @Override
    public PageResult<Course> searchPendingCourses(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return toPageResult(jpaCourseRepository.searchPending(normalizeKeyword(keyword), pageable));
    }

    @Override
    public PageResult<Course> searchAllCourses(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return toPageResult(jpaCourseRepository.searchAll(normalizeKeyword(keyword), pageable));
    }

    @Override
    public PageResult<Course> searchByInstructorId(Long instructorId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return toPageResult(jpaCourseRepository.searchByInstructorId(instructorId, normalizeKeyword(keyword), pageable));
    }

    @Override
    public Optional<Course> findById(Long id) {
        return jpaCourseRepository.findById(id).map(courseMapper::toDomain);
    }

    @Override
    public Optional<Course> findByIdForUpdate(Long id) {
        return jpaCourseRepository.findByIdForUpdate(id).map(courseMapper::toDomain);
    }

    @Override
    public Course save(Course course) {
        CourseJpaEntity entity = courseMapper.fromDomain(course);
        CourseJpaEntity saved = jpaCourseRepository.save(entity);
        return courseMapper.toDomain(saved);
    }

    @Override
    public void deleteById(Long id) {
        jpaCourseRepository.deleteById(id);
    }
}
