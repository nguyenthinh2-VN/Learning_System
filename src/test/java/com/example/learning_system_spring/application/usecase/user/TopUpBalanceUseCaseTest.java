package com.example.learning_system_spring.application.usecase.user;

import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.application.usecase.User.TopUpBalanceUseCase;
import com.example.learning_system_spring.domain.exception.UserNotFoundException;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TopUpBalanceUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TopUpBalanceUseCase topUpBalanceUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_ShouldTopUpSuccessfully_WhenUserExists() {
        // Arrange
        Role memberRole = Role.reconstitute(1L, "MEMBER", "Học viên");
        User user = User.reconstitute(1L, "MEM123", "user@test.com", "pass", "User", memberRole, false, new BigDecimal("100.00"), null, null);

        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        BigDecimal newBalance = topUpBalanceUseCase.execute(1L, new BigDecimal("500.00"));

        // Assert
        assertEquals(new BigDecimal("600.00"), newBalance);
        assertEquals(new BigDecimal("600.00"), user.getBalance());
        verify(userRepository).save(user);
    }

    @Test
    void execute_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> topUpBalanceUseCase.execute(99L, new BigDecimal("500.00")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void execute_ShouldThrowException_WhenAmountIsNegative() {
        // Arrange
        Role memberRole = Role.reconstitute(1L, "MEMBER", "Học viên");
        User user = User.reconstitute(1L, "MEM123", "user@test.com", "pass", "User", memberRole, false, new BigDecimal("100.00"), null, null);

        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> topUpBalanceUseCase.execute(1L, new BigDecimal("-50.00")));
        assertEquals("Nạp tiền phải lớn hơn 0", exception.getMessage());
        verify(userRepository, never()).save(any());
    }
}
