package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.role_permissionEntity.PermissionJpaEntity;
import com.example.learning_system_spring.adapter.repository.jpa.role_permissionEntity.RoleJpaEntity;
import com.example.learning_system_spring.adapter.repository.jpa.role_permissionEntity.RolePermissionJpaEntity;
import com.example.learning_system_spring.application.repository.RolePermissionRepository;
import com.example.learning_system_spring.domain.exception.PermissionNotFoundException;
import com.example.learning_system_spring.domain.exception.RoleNotFoundException;
import com.example.learning_system_spring.domain.model.RolePermissions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class RolePermissionRepositoryImpl implements RolePermissionRepository {

    private final JpaRolePermissionRepository jpaRolePermissionRepo;
    private final JpaRoleRepository jpaRoleRepo;
    private final JpaPermissionRepository jpaPermissionRepo;

    @Override
    public RolePermissions findByRoleName(String roleName) {
        Set<String> perms = jpaRolePermissionRepo.findByRole_Name(roleName).stream()
                .map(rp -> rp.getPermission().getName())
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        return RolePermissions.of(roleName, perms);
    }

    @Override
    public List<RolePermissions> findMatrix() {
        return jpaRoleRepo.findAll().stream()
                .map(role -> {
                    Set<String> perms = jpaRolePermissionRepo.findByRole_Name(role.getName()).stream()
                            .map(rp -> rp.getPermission().getName())
                            .collect(LinkedHashSet::new, Set::add, Set::addAll);
                    return RolePermissions.of(role.getName(), perms);
                })
                .toList();
    }

    @Override
    @Transactional
    public void replacePermissions(String roleName, List<String> permissionNames) {
        RoleJpaEntity role = jpaRoleRepo.findByName(roleName)
                .orElseThrow(() -> new RoleNotFoundException(roleName));

        // Xóa toàn bộ row cũ của role trong cùng transaction.
        jpaRolePermissionRepo.deleteByRole_Id(role.getId());
        // Đảm bảo DELETE chạy trước các INSERT để tránh vi phạm unique (role_id, permission_id).
        jpaRolePermissionRepo.flush();

        // Khử trùng lặp, giữ thứ tự.
        Set<String> distinct = new LinkedHashSet<>(permissionNames);
        for (String permName : distinct) {
            PermissionJpaEntity permission = jpaPermissionRepo.findByName(permName)
                    .orElseThrow(() -> new PermissionNotFoundException(permName));
            jpaRolePermissionRepo.save(RolePermissionJpaEntity.link(role, permission));
        }
    }
}
