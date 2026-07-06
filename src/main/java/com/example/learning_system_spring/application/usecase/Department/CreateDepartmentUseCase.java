package com.example.learning_system_spring.application.usecase.Department;

import com.example.learning_system_spring.application.dto.Department.CreateDepartmentInput;
import com.example.learning_system_spring.application.dto.Department.DepartmentOutput;
import com.example.learning_system_spring.application.repository.Department.DepartmentRepository;
import com.example.learning_system_spring.domain.model.Department;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateDepartmentUseCase {

    private final DepartmentRepository departmentRepository;

    public CreateDepartmentUseCase(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public DepartmentOutput execute(CreateDepartmentInput input) {
        if (departmentRepository.existsByCode(input.code())) {
            throw new IllegalArgumentException("Department code already exists: " + input.code());
        }

        if (input.parentId() != null) {
            departmentRepository.findById(input.parentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent department not found"));
        }

        Department department = new Department(input.code(), input.name(), input.parentId());
        Department saved = departmentRepository.save(department);

        return DepartmentOutput.fromDomain(saved);
    }
}
