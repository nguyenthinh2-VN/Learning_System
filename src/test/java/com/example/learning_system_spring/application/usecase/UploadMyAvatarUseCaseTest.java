package com.example.learning_system_spring.application.usecase;

import com.example.learning_system_spring.application.dto.User.UploadAvatarInput;
import com.example.learning_system_spring.application.dto.User.UserProfileOutput;
import com.example.learning_system_spring.application.port.FileStorageService;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.application.usecase.User.UploadMyAvatarUseCase;
import com.example.learning_system_spring.domain.exception.InvalidFileTypeException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UploadMyAvatarUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private UploadMyAvatarUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private User buildUser() {
        Role role = Role.reconstitute(1L, "MEMBER", "Học viên");
        return User.reconstitute(1L, "MEM123", "user@test.com", "hash", "Test User",
                role, false, BigDecimal.ZERO, null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void execute_UploadPngSuccessfully() {
        User user = buildUser();
        byte[] content = new byte[]{1, 2, 3};
        when(fileStorageService.store(content, "image/png", "avatars"))
                .thenReturn("http://host/uploads/avatars/abc.png");
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileOutput output = useCase.execute(new UploadAvatarInput(1L, content, "image/png"));

        assertEquals("http://host/uploads/avatars/abc.png", output.avatarUrl());
        verify(fileStorageService).store(content, "image/png", "avatars");
        verify(userRepository).save(user);
    }

    @Test
    void execute_InvalidContentType_ThrowsException() {
        byte[] content = new byte[]{1, 2, 3};

        assertThrows(InvalidFileTypeException.class,
                () -> useCase.execute(new UploadAvatarInput(1L, content, "application/pdf")));
        verify(fileStorageService, never()).store(any(), any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void execute_EmptyContent_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new UploadAvatarInput(1L, new byte[]{}, "image/png")));
        verify(fileStorageService, never()).store(any(), any(), any());
    }

    @Test
    void execute_AcceptsJpegAndWebp() {
        User user = buildUser();
        byte[] content = new byte[]{9};
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(fileStorageService.store(eq(content), eq("image/jpeg"), eq("avatars")))
                .thenReturn("http://host/uploads/avatars/x.jpg");

        UserProfileOutput out = useCase.execute(new UploadAvatarInput(1L, content, "image/jpeg"));
        assertEquals("http://host/uploads/avatars/x.jpg", out.avatarUrl());
    }
}
