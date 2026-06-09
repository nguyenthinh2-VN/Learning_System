package com.example.learning_system_spring.application.usecase.User;

import com.example.learning_system_spring.application.dto.User.UserProfileOutput;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.domain.exception.UserNotFoundException;
import com.example.learning_system_spring.domain.model.User;
import com.example.learning_system_spring.infrastructure.config.PermissionCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetMyProfileUseCase {

    private final UserRepository userRepository;
    private final PermissionCacheService permissionCacheService;

    @Transactional(readOnly = true)
    public UserProfileOutput execute(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        List<String> permissions = permissionCacheService.getPermissions(user.getRole().getName())
                .stream().sorted().toList();
        return UserProfileOutput.from(user, permissions);
    }
}
