package com.example.learning_system_spring.application.usecase.permission;

import com.example.learning_system_spring.application.dto.Permission.RolePermissionsOutput;
import com.example.learning_system_spring.application.dto.Permission.UpdateRolePermissionsInput;
import com.example.learning_system_spring.application.repository.PermissionRepository;
import com.example.learning_system_spring.application.repository.RolePermissionRepository;
import com.example.learning_system_spring.application.repository.RoleRepository;
import com.example.learning_system_spring.application.usecase.Permission.UpdateRolePermissionsUseCase;
import com.example.learning_system_spring.domain.exception.PermissionNotFoundException;
import com.example.learning_system_spring.domain.exception.ProtectedRoleException;
import com.example.learning_system_spring.domain.exception.RoleNotFoundException;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.model.RolePermissions;
import com.example.learning_system_spring.infrastructure.config.PermissionCacheService;
import com.example.learning_system_spring.infrastructure.service.PermissionAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UpdateRolePermissionsUseCaseTest {

    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private RolePermissionRepository rolePermissionRepository;
    @Mock private PermissionCacheService permissionCacheService;
    @Mock private PermissionAuditService permissionAuditService;
    @InjectMocks private UpdateRolePermissionsUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Replace-all thành công: validate, lưu, reload cache, ghi audit")
    void happyPath() {
        when(roleRepository.findByName("STAFF"))
                .thenReturn(Optional.of(Role.reconstitute(3L, "STAFF", null)));
        when(permissionRepository.existsByName(anyString())).thenReturn(true);
        when(rolePermissionRepository.findByRoleName("STAFF"))
                .thenReturn(RolePermissions.of("STAFF", Set.of("VIEW_COURSE")));

        RolePermissionsOutput out = useCase.execute(new UpdateRolePermissionsInput(
                "STAFF", List.of("VIEW_COURSE", "PUBLISH_COURSE"), 1L));

        assertThat(out.roleName()).isEqualTo("STAFF");
        assertThat(out.permissions()).containsExactlyInAnyOrder("VIEW_COURSE", "PUBLISH_COURSE");
        assertThat(out.locked()).isFalse();

        verify(rolePermissionRepository).replacePermissions(eq("STAFF"), any());
        verify(permissionCacheService).reload();
        verify(permissionAuditService).logRolePermissionsUpdated(
                eq(1L), eq("STAFF"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("SUPER_ADMIN bị bảo vệ → ProtectedRoleException, không lưu/không reload")
    void superAdminProtected() {
        when(roleRepository.findByName("SUPER_ADMIN"))
                .thenReturn(Optional.of(Role.reconstitute(5L, "SUPER_ADMIN", null)));

        assertThatThrownBy(() -> useCase.execute(new UpdateRolePermissionsInput(
                "SUPER_ADMIN", List.of("VIEW_COURSE"), 1L)))
                .isInstanceOf(ProtectedRoleException.class);

        verify(rolePermissionRepository, never()).replacePermissions(anyString(), any());
        verify(permissionCacheService, never()).reload();
    }

    @Test
    @DisplayName("Role không tồn tại → RoleNotFoundException")
    void roleNotFound() {
        when(roleRepository.findByName("GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UpdateRolePermissionsInput(
                "GHOST", List.of("VIEW_COURSE"), 1L)))
                .isInstanceOf(RoleNotFoundException.class);
        verify(rolePermissionRepository, never()).replacePermissions(anyString(), any());
    }

    @Test
    @DisplayName("Permission không tồn tại → PermissionNotFoundException, không lưu")
    void permissionNotFound() {
        when(roleRepository.findByName("STAFF"))
                .thenReturn(Optional.of(Role.reconstitute(3L, "STAFF", null)));
        when(permissionRepository.existsByName("VIEW_COURSE")).thenReturn(true);
        when(permissionRepository.existsByName("GHOST_PERM")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new UpdateRolePermissionsInput(
                "STAFF", List.of("VIEW_COURSE", "GHOST_PERM"), 1L)))
                .isInstanceOf(PermissionNotFoundException.class);
        verify(rolePermissionRepository, never()).replacePermissions(anyString(), any());
        verify(permissionCacheService, never()).reload();
    }

    @Test
    @DisplayName("Danh sách rỗng hợp lệ: gỡ hết quyền của role")
    void emptyListClearsPermissions() {
        when(roleRepository.findByName("MEMBER"))
                .thenReturn(Optional.of(Role.reconstitute(1L, "MEMBER", null)));
        when(rolePermissionRepository.findByRoleName("MEMBER"))
                .thenReturn(RolePermissions.of("MEMBER", Set.of("VIEW_COURSE")));

        RolePermissionsOutput out = useCase.execute(new UpdateRolePermissionsInput(
                "MEMBER", List.of(), 1L));

        assertThat(out.permissions()).isEmpty();
        verify(rolePermissionRepository).replacePermissions(eq("MEMBER"), eq(List.of()));
        verify(permissionCacheService).reload();
    }
}
