package com.example.learning_system_spring.application.usecase.Cart;

import com.example.learning_system_spring.application.repository.Cart.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RemoveCourseFromCartUseCase {

    private final CartItemRepository cartItemRepository;

    @Transactional
    public void remove(Long userId, Long courseId) {
        cartItemRepository.deleteByUserIdAndCourseId(userId, courseId);
    }

    @Transactional
    public void removeMultiple(Long userId, List<Long> courseIds) {
        cartItemRepository.deleteByUserIdAndCourseIdIn(userId, courseIds);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteAllByUserId(userId);
    }
}
