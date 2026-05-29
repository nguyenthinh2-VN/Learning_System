package com.example.learning_system_spring.application.usecase.User;

import com.example.learning_system_spring.application.dto.User.ChangePasswordInput;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.domain.exception.InvalidPasswordException;
import com.example.learning_system_spring.domain.exception.UserNotFoundException;
import com.example.learning_system_spring.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangeMyPasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void execute(ChangePasswordInput input) {
        User user = userRepository.findByIdForUpdate(input.userId())
                .orElseThrow(() -> new UserNotFoundException(input.userId()));

        // 1. Mật khẩu hiện tại phải khớp hash đang lưu
        if (!passwordEncoder.matches(input.currentPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        // 2. Mật khẩu mới phải khác mật khẩu cũ
        if (passwordEncoder.matches(input.newPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu cũ.");
        }

        // 3. Encode rồi lưu (không bao giờ lưu plaintext)
        String encoded = passwordEncoder.encode(input.newPassword());
        user.changePassword(encoded);
        userRepository.save(user);
    }
}
