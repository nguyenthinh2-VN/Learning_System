package com.example.learning_system_spring.application.usecase.Cart;

import com.example.learning_system_spring.application.dto.Cart.CartItemOutput;
import com.example.learning_system_spring.application.dto.Cart.CartOutput;
import com.example.learning_system_spring.application.repository.Cart.CartItemRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.domain.model.CartItem;
import com.example.learning_system_spring.domain.model.Course;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetCartItemsUseCase {

    private final CartItemRepository cartItemRepository;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public CartOutput execute(Long userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);

        List<CartItemOutput> items = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CartItem item : cartItems) {
            Optional<Course> courseOpt = courseRepository.findById(item.getCourseId());
            if (courseOpt.isPresent()) {
                Course course = courseOpt.get();
                // Bỏ qua nếu course bị gỡ
                if (course.isPublished()) {
                    BigDecimal price = course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;
                    items.add(new CartItemOutput(
                            course.getId(),
                            course.getTitle(),
                            price,
                            course.getThumbnailUrl(),
                            item.getAddedAt()
                    ));
                    totalPrice = totalPrice.add(price);
                }
            }
        }

        return new CartOutput(items, totalPrice);
    }
}
