package com.example.learning_system_spring.application.usecase.User;

import com.example.learning_system_spring.application.dto.CreateUserInput;
import com.example.learning_system_spring.application.dto.Auth.RegisterOutput;
import com.example.learning_system_spring.application.repository.RoleRepository;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.domain.exception.EmailAlreadyExistsException;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.model.User;
import com.example.learning_system_spring.application.usecase.strategy.user.UsernameGeneratorFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminCreateUserUseCase {
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final UsernameGeneratorFactory usernameGeneratorFactory;

    @Transactional
    public RegisterOutput execute(CreateUserInput input) {
        if (userRepo.existsByEmail(input.email())) {
            throw new EmailAlreadyExistsException(input.email());
        }

        Role role = roleRepo.findByName(input.roleName().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + input.roleName()));

        String username = usernameGeneratorFactory.generateUsername(role.getName());

        String encodedPassword = passwordEncoder.encode(input.password());

        User user = User.create(username, input.email(), encodedPassword, input.name(), role, input.isInternal());
        User saved = userRepo.save(user);

        return RegisterOutput.from(saved);
    }
}
