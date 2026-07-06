package com.example.learning_system_spring.application.usecase.Department;

import com.example.learning_system_spring.application.repository.Department.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteDepartmentUseCase {

    private final DepartmentRepository departmentRepository;

    public DeleteDepartmentUseCase(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public void execute(Long id) {
        if (!departmentRepository.findByParentId(id).isEmpty()) {
            throw new IllegalArgumentException("Cannot delete department because it has child departments");
        }
        
        // TODO: We will also need to check if any User or Course is using this department before deleting.
        // For now, we allow deletion assuming the repository will throw ConstraintViolationException if used.
        departmentRepository.deleteById(id);
    }
}
