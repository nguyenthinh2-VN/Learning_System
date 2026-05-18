package com.example.learning_system_spring.application.usecase;

import com.example.learning_system_spring.application.dto.CreateUserInput;
import com.example.learning_system_spring.application.dto.RegisterOutput;
import com.example.learning_system_spring.application.repository.RoleRepository;
import com.example.learning_system_spring.application.repository.UserRepository;
import com.example.learning_system_spring.application.usecase.strategy.user.UsernameGeneratorFactory;
import com.example.learning_system_spring.domain.exception.EmailAlreadyExistsException;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminCreateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UsernameGeneratorFactory usernameGeneratorFactory;

    @InjectMocks
    private AdminCreateUserUseCase adminCreateUserUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_SuccessfulCreation() {
        // Arrange
        CreateUserInput input = new CreateUserInput("gv@test.com", "password", "GV Test", "INSTRUCTOR", true);
        Role instructorRole = Role.reconstitute(2L, "INSTRUCTOR", "Giảng viên");

        when(userRepository.existsByEmail("gv@test.com")).thenReturn(false);
        when(roleRepository.findByName("INSTRUCTOR")).thenReturn(Optional.of(instructorRole));
        when(usernameGeneratorFactory.generateUsername("INSTRUCTOR")).thenReturn("GV123456");
        when(passwordEncoder.encode("password")).thenReturn("encoded");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        RegisterOutput output = adminCreateUserUseCase.execute(input);

        // Assert
        assertNotNull(output);
        assertEquals("gv@test.com", output.email());
        assertEquals("GV123456", output.username());
        assertEquals("INSTRUCTOR", output.role());
        assertTrue(output.isInternal());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("GV Test", userCaptor.getValue().getName());
    }

    @Test
    void execute_EmailAlreadyExists_ThrowsException() {
        // Arrange
        CreateUserInput input = new CreateUserInput("gv@test.com", "password", "GV Test", "INSTRUCTOR", true);
        when(userRepository.existsByEmail("gv@test.com")).thenReturn(true);

        // Act & Assert
        assertThrows(EmailAlreadyExistsException.class, () -> adminCreateUserUseCase.execute(input));
        verify(userRepository, never()).save(any());
    }
}
