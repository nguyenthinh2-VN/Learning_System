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

    @Override
    public PageResult<Course> searchCourses(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CourseJpaEntity> entityPage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            entityPage = jpaCourseRepository.searchByKeyword(keyword.trim(), pageable);
        } else {
            entityPage = jpaCourseRepository.findAll(pageable);
        }

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
