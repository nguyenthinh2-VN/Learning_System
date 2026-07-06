package com.example.learning_system_spring.application.usecase.Department;

import com.example.learning_system_spring.application.dto.Department.DepartmentOutput;
import com.example.learning_system_spring.application.dto.Department.UpdateDepartmentInput;
import com.example.learning_system_spring.application.repository.Department.DepartmentRepository;
import com.example.learning_system_spring.domain.model.Department;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class UpdateDepartmentUseCase {

    private final DepartmentRepository departmentRepository;

    public UpdateDepartmentUseCase(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public DepartmentOutput execute(UpdateDepartmentInput input) {
        Department department = departmentRepository.findById(input.id())
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        if (departmentRepository.existsByCodeAndIdNot(input.code(), input.id())) {
            throw new IllegalArgumentException("Department code already exists: " + input.code());
        }

        if (input.parentId() != null) {
            if (Objects.equals(input.parentId(), input.id())) {
                throw new IllegalArgumentException("Department cannot be its own parent");
            }
            departmentRepository.findById(input.parentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent department not found"));
        }

        department.update(input.code(), input.name(), input.parentId());
        Department saved = departmentRepository.save(department);

        return DepartmentOutput.fromDomain(saved);
    }
}
