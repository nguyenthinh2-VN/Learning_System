package com.example.learning_system_spring.application.usecase.Auth;

import com.example.learning_system_spring.application.dto.Auth.RegisterInput;
import com.example.learning_system_spring.application.dto.Auth.RegisterOutput;
import com.example.learning_system_spring.application.repository.RoleRepository;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.domain.exception.EmailAlreadyExistsException;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterUseCase {
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisterOutput execute(RegisterInput input) {
        if (userRepo.existsByEmail(input.email())) {
            throw new EmailAlreadyExistsException(input.email());
        }

        Role memberRole = roleRepo.findByName("MEMBER")
            .orElseThrow(() -> new IllegalStateException("Default role MEMBER not found"));

        String encodedPassword = passwordEncoder.encode(input.password());
        
        // Sinh username cho self-registration
        String username = "MEM" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        // Đăng ký tự do luôn là external (isInternal = false)
        User user = User.create(username, input.email(), encodedPassword, input.name(), memberRole, false);
        User saved = userRepo.save(user);

        return RegisterOutput.from(saved);
    }
}
