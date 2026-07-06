package com.example.learning_system_spring.adapter.controller.Department;

import com.example.learning_system_spring.application.dto.Department.CreateDepartmentInput;
import com.example.learning_system_spring.application.dto.Department.DepartmentOutput;
import com.example.learning_system_spring.application.dto.Department.UpdateDepartmentInput;
import com.example.learning_system_spring.application.usecase.Department.CreateDepartmentUseCase;
import com.example.learning_system_spring.application.usecase.Department.DeleteDepartmentUseCase;
import com.example.learning_system_spring.application.usecase.Department.GetDepartmentsUseCase;
import com.example.learning_system_spring.application.usecase.Department.UpdateDepartmentUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/departments")
@PreAuthorize("hasAuthority('MANAGE_DEPARTMENT')")
public class DepartmentAdminController {

    private final CreateDepartmentUseCase createDepartmentUseCase;
    private final UpdateDepartmentUseCase updateDepartmentUseCase;
    private final DeleteDepartmentUseCase deleteDepartmentUseCase;
    private final GetDepartmentsUseCase getDepartmentsUseCase;

    public DepartmentAdminController(CreateDepartmentUseCase createDepartmentUseCase,
            UpdateDepartmentUseCase updateDepartmentUseCase,
            DeleteDepartmentUseCase deleteDepartmentUseCase,
            GetDepartmentsUseCase getDepartmentsUseCase) {
        this.createDepartmentUseCase = createDepartmentUseCase;
        this.updateDepartmentUseCase = updateDepartmentUseCase;
        this.deleteDepartmentUseCase = deleteDepartmentUseCase;
        this.getDepartmentsUseCase = getDepartmentsUseCase;
    }

    @GetMapping
    public ResponseEntity<?> getAllDepartments() {
        List<DepartmentOutput> departments = getDepartmentsUseCase.execute();
        return ResponseEntity.ok(Map.of("data", departments));
    }

    @PostMapping
    public ResponseEntity<?> createDepartment(@RequestBody CreateDepartmentInput input) {
        try {
            DepartmentOutput created = createDepartmentUseCase.execute(input);
            return ResponseEntity.ok(Map.of("data", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDepartment(@PathVariable Long id, @RequestBody UpdateDepartmentInput request) {
        try {
            UpdateDepartmentInput input = new UpdateDepartmentInput(id, request.code(), request.name(),
                    request.parentId());
            DepartmentOutput updated = updateDepartmentUseCase.execute(input);
            return ResponseEntity.ok(Map.of("data", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDepartment(@PathVariable Long id) {
        try {
            deleteDepartmentUseCase.execute(id);
            return ResponseEntity.ok(Map.of("message", "Department deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
