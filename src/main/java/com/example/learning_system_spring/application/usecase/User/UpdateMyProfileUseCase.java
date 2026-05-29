package com.example.learning_system_spring.application.usecase.User;

import com.example.learning_system_spring.application.dto.User.UpdateMyProfileInput;
import com.example.learning_system_spring.application.dto.User.UserProfileOutput;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.domain.exception.UserNotFoundException;
import com.example.learning_system_spring.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateMyProfileUseCase {

    private final UserRepository userRepository;

    @Transactional
    public UserProfileOutput execute(UpdateMyProfileInput input) {
        // Pessimistic lock để tránh ghi đè đồng thời (nhất quán với TopUpBalanceUseCase)
        User user = userRepository.findByIdForUpdate(input.userId())
                .orElseThrow(() -> new UserNotFoundException(input.userId()));

        user.changeName(input.name());
        user.changeAvatar(input.avatarUrl()); // null = giữ nguyên, "" = xóa

        User saved = userRepository.save(user);
        return UserProfileOutput.from(saved);
    }
}
