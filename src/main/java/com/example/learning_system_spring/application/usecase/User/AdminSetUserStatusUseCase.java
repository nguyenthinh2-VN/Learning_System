package com.example.learning_system_spring.application.usecase.User;

import com.example.learning_system_spring.application.dto.User.AdminUpdateUserStatusInput;
import com.example.learning_system_spring.application.dto.User.AdminUserDetailOutput;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.domain.exception.UserNotFoundException;
import com.example.learning_system_spring.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin khóa / mở khóa tài khoản user.
 * Quy tắc bảo mật:
 *  - Không tự khóa chính mình.
 *  - ADMIN_USER không được khóa SUPER_ADMIN.
 */
@Service
@RequiredArgsConstructor
public class AdminSetUserStatusUseCase {

    private final UserRepository userRepository;

    @Transactional
    public AdminUserDetailOutput execute(AdminUpdateUserStatusInput input) {
        User target = userRepository.findByIdForUpdate(input.targetUserId())
                .orElseThrow(() -> new UserNotFoundException(input.targetUserId()));

        boolean requesterIsSuperAdmin = "SUPER_ADMIN".equals(input.requesterRole());

        // ADMIN_USER không được khóa SUPER_ADMIN
        if (target.getRole().isSuperAdmin() && !requesterIsSuperAdmin) {
            throw new IllegalStateException("Bạn không có quyền khóa tài khoản SUPER_ADMIN.");
        }

        // Không tự khóa chính mình
        if (!input.enabled() && target.getId().equals(input.requesterId())) {
            throw new IllegalStateException("Bạn không thể tự khóa tài khoản của chính mình.");
        }

        if (input.enabled()) {
            target.enable();
        } else {
            target.disable();
        }

        User saved = userRepository.save(target);
        return AdminUserDetailOutput.from(saved);
    }
}
