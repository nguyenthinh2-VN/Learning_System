package com.example.learning_system_spring.application.usecase.Department;

import com.example.learning_system_spring.application.dto.Department.DepartmentOutput;
import com.example.learning_system_spring.application.repository.Department.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GetDepartmentsUseCase {

    private final DepartmentRepository departmentRepository;

    public GetDepartmentsUseCase(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<DepartmentOutput> execute() {
        return departmentRepository.findAll().stream()
                .map(DepartmentOutput::fromDomain)
                .collect(Collectors.toList());
    }
}
