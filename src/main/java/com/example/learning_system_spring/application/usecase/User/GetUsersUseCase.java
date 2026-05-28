package com.example.learning_system_spring.application.usecase.User;

import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.application.dto.User.UserListOutput;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetUsersUseCase {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResult<UserListOutput> execute(String keyword, int page, int size) {
        return userRepository.findAll(keyword, page, size)
                .map(user -> new UserListOutput(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getName(),
                        user.getRole().getName(),
                        user.isInternal(),
                        user.getCreatedAt()
                ));
    }
}
