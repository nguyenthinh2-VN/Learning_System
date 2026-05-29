package com.example.learning_system_spring.application.usecase;

import com.example.learning_system_spring.application.dto.User.UpdateMyProfileInput;
import com.example.learning_system_spring.application.dto.User.UserProfileOutput;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.application.usecase.User.UpdateMyProfileUseCase;
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

class UpdateMyProfileUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UpdateMyProfileUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private User buildUser() {
        Role role = Role.reconstitute(1L, "MEMBER", "Học viên");
        return User.reconstitute(1L, "MEM123", "user@test.com", "encodedPassword", "Old Name",
                role, false, BigDecimal.ZERO, "http://host/old.png", LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void execute_ChangeNameSuccessfully() {
        User user = buildUser();
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileOutput output = useCase.execute(new UpdateMyProfileInput(1L, "New Name", null));

        assertEquals("New Name", output.name());
        // avatarUrl null = giữ nguyên
        assertEquals("http://host/old.png", output.avatarUrl());
        verify(userRepository).save(user);
    }

    @Test
    void execute_EmptyAvatar_ClearsAvatar() {
        User user = buildUser();
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileOutput output = useCase.execute(new UpdateMyProfileInput(1L, "New Name", ""));

        assertNull(output.avatarUrl());
    }

    @Test
    void execute_NewAvatarUrl_Updates() {
        User user = buildUser();
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileOutput output = useCase.execute(
                new UpdateMyProfileInput(1L, "New Name", "http://host/new.png"));

        assertEquals("http://host/new.png", output.avatarUrl());
    }

    @Test
    void execute_UserNotFound_ThrowsException() {
        when(userRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> useCase.execute(new UpdateMyProfileInput(99L, "Name", null)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void execute_BlankName_ThrowsException() {
        User user = buildUser();
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new UpdateMyProfileInput(1L, "   ", null)));
        verify(userRepository, never()).save(any());
    }
}
