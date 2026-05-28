package com.example.learning_system_spring.adapter.mapper;

import com.example.learning_system_spring.adapter.repository.jpa.CourseEntity.CourseJpaEntity;
import com.example.learning_system_spring.adapter.repository.jpa.CourseEntity.CourseLessonJpaEntity;
import com.example.learning_system_spring.adapter.repository.jpa.CourseEntity.CourseSectionJpaEntity;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.CourseLesson;
import com.example.learning_system_spring.domain.model.CourseSection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CourseMapper {

    public Course toDomain(CourseJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        List<CourseSection> sections = entity.getSections().stream()
                .map(this::toDomainSection)
                .collect(Collectors.toList());

        return Course.reconstitute(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getMaxStudents(),
                entity.getEnrolledCount(),
                entity.getPrice(),
                entity.getInstructorId(),
                entity.getThumbnailUrl(),
                entity.isPublished(),
                entity.isPriceLocked(),
                entity.getPublishedAt(),
                entity.getPublishedBy(),
                sections);
    }

    private CourseSection toDomainSection(CourseSectionJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        List<CourseLesson> lessons = entity.getLessons().stream()
                .map(this::toDomainLesson)
                .collect(Collectors.toList());

        return CourseSection.reconstitute(
                entity.getId(),
                entity.getTitle(),
                entity.getOrderIndex(),
                lessons);
    }

    private CourseLesson toDomainLesson(CourseLessonJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return CourseLesson.reconstitute(
                entity.getId(),
                entity.getTitle(),
                entity.getContentUrl(),
                entity.getOrderIndex());
    }

    public CourseJpaEntity fromDomain(Course course) {
        if (course == null) {
            return null;
        }

        CourseJpaEntity entity = new CourseJpaEntity();
        entity.setId(course.getId());
        entity.setTitle(course.getTitle());
        entity.setDescription(course.getDescription());
        entity.setMaxStudents(course.getMaxStudents());
        entity.setEnrolledCount(course.getEnrolledCount());
        entity.setPrice(course.getPrice());
        entity.setInstructorId(course.getInstructorId());
        entity.setThumbnailUrl(course.getThumbnailUrl());
        entity.setPublished(course.isPublished());
        entity.setPriceLocked(course.isPriceLocked());
        entity.setPublishedAt(course.getPublishedAt());
        entity.setPublishedBy(course.getPublishedBy());

        if (course.getSections() != null) {
            course.getSections().forEach(sectionDomain -> {
                CourseSectionJpaEntity sectionEntity = fromDomainSection(sectionDomain);
                entity.addSection(sectionEntity);
            });
        }

        return entity;
    }

    private CourseSectionJpaEntity fromDomainSection(CourseSection section) {
        if (section == null) {
            return null;
        }

        CourseSectionJpaEntity entity = new CourseSectionJpaEntity();
        entity.setId(section.getId());
        entity.setTitle(section.getTitle());
        entity.setOrderIndex(section.getOrderIndex());

        if (section.getLessons() != null) {
            section.getLessons().forEach(lessonDomain -> {
                CourseLessonJpaEntity lessonEntity = fromDomainLesson(lessonDomain);
                entity.addLesson(lessonEntity);
            });
        }

        return entity;
    }

    private CourseLessonJpaEntity fromDomainLesson(CourseLesson lesson) {
        if (lesson == null) {
            return null;
        }

        CourseLessonJpaEntity entity = new CourseLessonJpaEntity();
        entity.setId(lesson.getId());
        entity.setTitle(lesson.getTitle());
        entity.setContentUrl(lesson.getContentUrl());
        entity.setOrderIndex(lesson.getOrderIndex());

        return entity;
    }
}
