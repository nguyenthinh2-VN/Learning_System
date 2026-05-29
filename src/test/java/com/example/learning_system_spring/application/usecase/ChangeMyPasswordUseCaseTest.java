package com.example.learning_system_spring.application.usecase;

import com.example.learning_system_spring.application.dto.User.ChangePasswordInput;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.application.usecase.User.ChangeMyPasswordUseCase;
import com.example.learning_system_spring.domain.exception.InvalidPasswordException;
import com.example.learning_system_spring.domain.exception.UserNotFoundException;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChangeMyPasswordUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ChangeMyPasswordUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private User buildUser() {
        Role role = Role.reconstitute(1L, "MEMBER", "Học viên");
        return User.reconstitute(1L, "MEM123", "user@test.com", "oldHash", "Test User",
                role, false, BigDecimal.ZERO, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void execute_ChangePasswordSuccessfully() {
        User user = buildUser();
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current", "oldHash")).thenReturn(true);
        when(passwordEncoder.matches("newPass", "oldHash")).thenReturn(false);
        when(passwordEncoder.encode("newPass")).thenReturn("newHash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(new ChangePasswordInput(1L, "current", "newPass"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("newHash", captor.getValue().getPassword());
    }

    @Test
    void execute_WrongCurrentPassword_ThrowsInvalidPassword() {
        User user = buildUser();
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "oldHash")).thenReturn(false);

        assertThrows(InvalidPasswordException.class,
                () -> useCase.execute(new ChangePasswordInput(1L, "wrong", "newPass")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void execute_NewPasswordSameAsCurrent_ThrowsException() {
        User user = buildUser();
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        // current khớp hash cũ, và newPass (== "current") cũng khớp → trùng mật khẩu cũ
        when(passwordEncoder.matches("current", "oldHash")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new ChangePasswordInput(1L, "current", "current")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void execute_UserNotFound_ThrowsException() {
        when(userRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> useCase.execute(new ChangePasswordInput(99L, "current", "newPass")));
    }
}
