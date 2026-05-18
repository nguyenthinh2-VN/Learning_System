package com.example.learning_system_spring.application.usecase.Course;

import com.example.learning_system_spring.application.dto.Course.GetCourseListInput;
import com.example.learning_system_spring.application.dto.Course.GetCourseListOutput;
import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.domain.model.Course;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetCourseListUseCase {
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public PageResult<GetCourseListOutput> execute(GetCourseListInput input) {
        PageResult<Course> coursesPage = courseRepository.searchCourses(input.keyword(), input.page(), input.size());

        List<GetCourseListOutput> items = coursesPage.items().stream()
                .map(GetCourseListOutput::from)
                .toList();

        return PageResult.of(
                coursesPage.totalElements(),
                coursesPage.totalPages(),
                coursesPage.page(),
                coursesPage.size(),
                items);
    }
}
