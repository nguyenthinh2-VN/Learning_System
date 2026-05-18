package com.example.learning_system_spring.application.usecase.Auth;

import com.example.learning_system_spring.application.dto.Auth.LoginInput;
import com.example.learning_system_spring.application.dto.Auth.LoginOutput;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.domain.exception.InvalidCredentialsException;
import com.example.learning_system_spring.domain.model.User;
import com.example.learning_system_spring.infrastructure.config.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginUseCase {
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public LoginOutput execute(LoginInput input) {
        User user = userRepo.findByUsernameOrEmail(input.identifier(), input.identifier())
            .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(input.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtService.generateToken(
            user.getId(), user.getUsername(), user.getEmail(), user.getRole().getName(), user.isInternal()
        );
        return LoginOutput.from(user, accessToken);
    }
}
