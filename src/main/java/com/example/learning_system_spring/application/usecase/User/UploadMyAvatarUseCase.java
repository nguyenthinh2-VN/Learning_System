package com.example.learning_system_spring.application.usecase.User;

import com.example.learning_system_spring.application.dto.User.UploadAvatarInput;
import com.example.learning_system_spring.application.dto.User.UserProfileOutput;
import com.example.learning_system_spring.application.port.FileStorageService;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.domain.exception.InvalidFileTypeException;
import com.example.learning_system_spring.domain.exception.UserNotFoundException;
import com.example.learning_system_spring.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UploadMyAvatarUseCase {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final String AVATAR_SUBDIR = "avatars";

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public UserProfileOutput execute(UploadAvatarInput input) {
        if (input.content() == null || input.content().length == 0) {
            throw new IllegalArgumentException("File ảnh không được rỗng.");
        }

        String contentType = input.contentType() == null ? "" : input.contentType().toLowerCase();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new InvalidFileTypeException(
                    "Định dạng ảnh không hợp lệ. Chỉ chấp nhận JPEG, PNG, WebP.");
        }

        // Lưu file trước (ngoài DB) → lấy public URL
        String url = fileStorageService.store(input.content(), contentType, AVATAR_SUBDIR);

        User user = userRepository.findByIdForUpdate(input.userId())
                .orElseThrow(() -> new UserNotFoundException(input.userId()));

        user.changeAvatar(url);
        User saved = userRepository.save(user);
        return UserProfileOutput.from(saved);
    }
}
