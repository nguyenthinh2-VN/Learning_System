package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.DepartmentEntity.DepartmentJpaEntity;
import com.example.learning_system_spring.application.repository.Department.DepartmentRepository;
import com.example.learning_system_spring.domain.model.Department;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class DepartmentRepositoryImpl implements DepartmentRepository {

    private final JpaDepartmentRepository jpaDepartmentRepository;

    public DepartmentRepositoryImpl(JpaDepartmentRepository jpaDepartmentRepository) {
        this.jpaDepartmentRepository = jpaDepartmentRepository;
    }

    @Override
    public Department save(Department department) {
        DepartmentJpaEntity entity = DepartmentJpaEntity.fromDomain(department);
        DepartmentJpaEntity savedEntity = jpaDepartmentRepository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    public Optional<Department> findById(Long id) {
        return jpaDepartmentRepository.findById(id).map(DepartmentJpaEntity::toDomain);
    }

    @Override
    public List<Department> findAll() {
        return jpaDepartmentRepository.findAll().stream()
                .map(DepartmentJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaDepartmentRepository.deleteById(id);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaDepartmentRepository.existsByCode(code);
    }

    @Override
    public boolean existsByCodeAndIdNot(String code, Long id) {
        return jpaDepartmentRepository.existsByCodeAndIdNot(code, id);
    }

    @Override
    public List<Department> findByParentId(Long parentId) {
        return jpaDepartmentRepository.findByParentId(parentId).stream()
                .map(DepartmentJpaEntity::toDomain)
                .collect(Collectors.toList());
    }
}
