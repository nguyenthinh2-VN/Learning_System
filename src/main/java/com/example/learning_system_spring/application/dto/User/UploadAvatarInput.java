package com.example.learning_system_spring.application.dto.User;

public record UploadAvatarInput(
        Long userId,
        byte[] content,
        String contentType
) {}
