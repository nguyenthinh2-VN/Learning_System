package com.example.learning_system_spring.application.usecase.Auth;

import com.example.learning_system_spring.application.dto.Auth.LoginInput;
import com.example.learning_system_spring.application.dto.Auth.LoginOutput;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.domain.exception.AccountDisabledException;
import com.example.learning_system_spring.domain.exception.InvalidCredentialsException;
import com.example.learning_system_spring.domain.model.User;
import com.example.learning_system_spring.infrastructure.config.JwtService;
import com.example.learning_system_spring.infrastructure.config.PermissionCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginUseCase {
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PermissionCacheService permissionCacheService;

    @Transactional(readOnly = true)
    public LoginOutput execute(LoginInput input) {
        User user = userRepo.findByUsernameOrEmail(input.identifier(), input.identifier())
            .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(input.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        if (!user.isEnabled()) {
            throw new AccountDisabledException();
        }

        String accessToken = jwtService.generateToken(
            user.getId(), user.getUsername(), user.getEmail(), user.getRole().getName(), user.isInternal()
        );
        List<String> permissions = permissionCacheService.getPermissions(user.getRole().getName())
            .stream().sorted().toList();
        return LoginOutput.from(user, accessToken, permissions);
    }
}
