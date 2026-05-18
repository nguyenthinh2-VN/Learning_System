package com.example.learning_system_spring.application.usecase;

import com.example.learning_system_spring.application.dto.RegisterInput;
import com.example.learning_system_spring.application.dto.RegisterOutput;
import com.example.learning_system_spring.application.repository.RoleRepository;
import com.example.learning_system_spring.application.repository.UserRepository;
import com.example.learning_system_spring.domain.exception.EmailAlreadyExistsException;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        User user = User.create(input.email(), encodedPassword, input.name(), memberRole);
        User saved = userRepo.save(user);

        return RegisterOutput.from(saved);
    }
}
