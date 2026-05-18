package com.example.learning_system_spring.application.usecase;

import com.example.learning_system_spring.application.dto.Auth.LoginInput;
import com.example.learning_system_spring.application.dto.Auth.LoginOutput;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.application.usecase.Auth.LoginUseCase;
import com.example.learning_system_spring.domain.exception.InvalidCredentialsException;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.model.User;
import com.example.learning_system_spring.infrastructure.config.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LoginUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private LoginUseCase loginUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_SuccessfulLogin() {
        // Arrange
        LoginInput input = new LoginInput("MEM123", "password");
        Role memberRole = Role.reconstitute(1L, "MEMBER", "Học viên");
        User user = User.reconstitute(1L, "MEM123", "user@test.com", "encodedPassword", "Test User", memberRole, false, java.math.BigDecimal.ZERO,
                LocalDateTime.now(), LocalDateTime.now());

        when(userRepository.findByUsernameOrEmail("MEM123", "MEM123")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken(1L, "MEM123", "user@test.com", "MEMBER", false)).thenReturn("mockToken");

        // Act
        LoginOutput output = loginUseCase.execute(input);

        // Assert
        assertNotNull(output);
        assertEquals("mockToken", output.accessToken());
        assertEquals("MEM123", output.username());
    }

    @Test
    void execute_UserNotFound_ThrowsException() {
        // Arrange
        LoginInput input = new LoginInput("wrongUser", "password");
        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> loginUseCase.execute(input));
    }

    @Test
    void execute_WrongPassword_ThrowsException() {
        // Arrange
        LoginInput input = new LoginInput("MEM123", "wrongPassword");
        Role memberRole = Role.reconstitute(1L, "MEMBER", "Học viên");
        User user = User.reconstitute(1L, "MEM123", "user@test.com", "encodedPassword", "Test User", memberRole, false, java.math.BigDecimal.ZERO,
                LocalDateTime.now(), LocalDateTime.now());

        when(userRepository.findByUsernameOrEmail("MEM123", "MEM123")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> loginUseCase.execute(input));
    }
}
