package com.example.learning_system_spring.application.repository.Department;

import com.example.learning_system_spring.domain.model.Department;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository {
    Department save(Department department);

    Optional<Department> findById(Long id);

    List<Department> findAll();

    void deleteById(Long id);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    List<Department> findByParentId(Long parentId);
}
