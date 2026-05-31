package com.example.learning_system_spring.application.usecase.user;

import com.example.learning_system_spring.application.dto.User.AdminUpdateUserStatusInput;
import com.example.learning_system_spring.application.dto.User.AdminUserDetailOutput;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.application.usecase.User.AdminSetUserStatusUseCase;
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

class AdminSetUserStatusUseCaseTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private AdminSetUserStatusUseCase useCase;

    private final Role memberRole = Role.reconstitute(1L, "MEMBER", null);
    private final Role superAdminRole = Role.reconstitute(5L, "SUPER_ADMIN", null);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private User user(Long id, Role role, boolean enabled) {
        return User.reconstitute(id, "u" + id, "u" + id + "@e.com", "hash", "Name " + id,
                role, false, BigDecimal.ZERO, null, enabled, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void execute_DisableSuccess() {
        User target = user(10L, memberRole, true);
        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserDetailOutput out = useCase.execute(
                new AdminUpdateUserStatusInput(10L, 1L, "SUPER_ADMIN", false));

        assertFalse(out.enabled());
    }

    @Test
    void execute_EnableSuccess() {
        User target = user(10L, memberRole, false);
        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserDetailOutput out = useCase.execute(
                new AdminUpdateUserStatusInput(10L, 1L, "SUPER_ADMIN", true));

        assertTrue(out.enabled());
    }

    @Test
    void execute_SelfLock_Blocked() {
        User target = user(10L, memberRole, true);
        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(target));

        assertThrows(IllegalStateException.class, () -> useCase.execute(
                new AdminUpdateUserStatusInput(10L, 10L, "SUPER_ADMIN", false)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void execute_AdminUserCannotLockSuperAdmin() {
        User target = user(10L, superAdminRole, true);
        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(target));

        assertThrows(IllegalStateException.class, () -> useCase.execute(
                new AdminUpdateUserStatusInput(10L, 1L, "ADMIN_USER", false)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void execute_UserNotFound_Throws() {
        when(userRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> useCase.execute(
                new AdminUpdateUserStatusInput(99L, 1L, "SUPER_ADMIN", false)));
    }
}
