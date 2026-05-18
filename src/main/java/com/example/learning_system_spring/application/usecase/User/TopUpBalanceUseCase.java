package com.example.learning_system_spring.application.usecase.User;

import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.domain.exception.UserNotFoundException;
import com.example.learning_system_spring.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TopUpBalanceUseCase {

    private final UserRepository userRepository;

    @Transactional
    public BigDecimal execute(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Nạp tiền phải lớn hơn 0");
        }

        // Use pessimistic locking to ensure consistent top-up in concurrent requests
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.addBalance(amount);
        User saved = userRepository.save(user);

        return saved.getBalance();
    }
}
