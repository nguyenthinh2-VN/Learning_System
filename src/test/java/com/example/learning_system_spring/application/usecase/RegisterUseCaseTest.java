package com.example.learning_system_spring.application.usecase;

import com.example.learning_system_spring.application.dto.RegisterInput;
import com.example.learning_system_spring.application.dto.RegisterOutput;
import com.example.learning_system_spring.application.repository.RoleRepository;
import com.example.learning_system_spring.application.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RegisterUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegisterUseCase registerUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_SuccessfulRegistration() {
        // Arrange
        RegisterInput input = new RegisterInput("user@test.com", "password", "Test User");
        Role memberRole = Role.reconstitute(1L, "MEMBER", "Học viên");
        
        when(userRepository.existsByEmail("user@test.com")).thenReturn(false);
        when(roleRepository.findByName("MEMBER")).thenReturn(Optional.of(memberRole));
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        RegisterOutput output = registerUseCase.execute(input);

        // Assert
        assertNotNull(output);
        assertEquals("user@test.com", output.email());
        assertEquals("MEMBER", output.role());
        assertTrue(output.username().startsWith("MEM"));
        assertFalse(output.isInternal());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("Test User", savedUser.getName());
        assertEquals("encodedPassword", savedUser.getPassword());
    }

    @Test
    void execute_EmailAlreadyExists_ThrowsException() {
        // Arrange
        RegisterInput input = new RegisterInput("user@test.com", "password", "Test User");
        when(userRepository.existsByEmail("user@test.com")).thenReturn(true);

        // Act & Assert
        assertThrows(EmailAlreadyExistsException.class, () -> registerUseCase.execute(input));
        verify(userRepository, never()).save(any());
    }
}
