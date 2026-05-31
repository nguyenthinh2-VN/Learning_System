package com.example.learning_system_spring.application.usecase.user;

import com.example.learning_system_spring.application.dto.User.AdminUpdateUserInput;
import com.example.learning_system_spring.application.dto.User.AdminUserDetailOutput;
import com.example.learning_system_spring.application.repository.RoleRepository;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.application.usecase.User.AdminUpdateUserUseCase;
import com.example.learning_system_spring.domain.exception.UserNotFoundException;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminUpdateUserUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @InjectMocks private AdminUpdateUserUseCase useCase;

    private final Role memberRole = Role.reconstitute(1L, "MEMBER", null);
    private final Role instructorRole = Role.reconstitute(2L, "INSTRUCTOR", null);
    private final Role superAdminRole = Role.reconstitute(5L, "SUPER_ADMIN", null);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private User user(Long id, Role role) {
        return User.reconstitute(id, "u" + id, "u" + id + "@e.com", "hash", "Name " + id,
                role, false, BigDecimal.ZERO, null, true, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void execute_UpdatesNameRoleInternal() {
        User target = user(10L, memberRole);
        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(target));
        when(roleRepository.findByName("INSTRUCTOR")).thenReturn(Optional.of(instructorRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserDetailOutput out = useCase.execute(
                new AdminUpdateUserInput(10L, 1L, "SUPER_ADMIN", "New Name", "INSTRUCTOR", true));

        assertEquals("New Name", out.name());
        assertEquals("INSTRUCTOR", out.role());
        assertTrue(out.isInternal());
    }

    @Test
    void execute_NullFields_KeepOldValues() {
        User target = user(10L, memberRole);
        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserDetailOutput out = useCase.execute(
                new AdminUpdateUserInput(10L, 1L, "SUPER_ADMIN", null, null, null));

        assertEquals("Name 10", out.name());
        assertEquals("MEMBER", out.role());
        assertFalse(out.isInternal());
        verify(roleRepository, never()).findByName(any());
    }

    @Test
    void execute_RoleNotFound_Throws() {
        User target = user(10L, memberRole);
        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(target));
        when(roleRepository.findByName("GHOST")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                new AdminUpdateUserInput(10L, 1L, "SUPER_ADMIN", null, "GHOST", null)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void execute_UserNotFound_Throws() {
        when(userRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> useCase.execute(
                new AdminUpdateUserInput(99L, 1L, "SUPER_ADMIN", "X", null, null)));
    }

    @Test
    void execute_SelfRoleChange_Blocked() {
        User target = user(10L, memberRole);
        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(target));

        // requesterId == targetId, đổi role chính mình
        assertThrows(IllegalStateException.class, () -> useCase.execute(
                new AdminUpdateUserInput(10L, 10L, "SUPER_ADMIN", null, "INSTRUCTOR", null)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void execute_AdminUserCannotEditSuperAdmin() {
        User target = user(10L, superAdminRole);
        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(target));

        assertThrows(IllegalStateException.class, () -> useCase.execute(
                new AdminUpdateUserInput(10L, 1L, "ADMIN_USER", "X", null, null)));
        verify(userRepository, never()).save(any());
    }
}
