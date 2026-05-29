package com.example.learning_system_spring.application.usecase.User;

import com.example.learning_system_spring.application.dto.User.UserProfileOutput;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.domain.exception.UserNotFoundException;
import com.example.learning_system_spring.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMyProfileUseCase {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileOutput execute(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return UserProfileOutput.from(user);
    }
}
