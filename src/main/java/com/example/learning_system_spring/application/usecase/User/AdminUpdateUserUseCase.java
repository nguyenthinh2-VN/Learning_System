package com.example.learning_system_spring.application.usecase.User;

import com.example.learning_system_spring.application.dto.User.AdminUpdateUserInput;
import com.example.learning_system_spring.application.dto.User.AdminUserDetailOutput;
import com.example.learning_system_spring.application.repository.RoleRepository;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.domain.exception.UserNotFoundException;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin cập nhật thông tin user: name, role, isInternal.
 * Quy tắc bảo mật:
 *  - Không tự đổi role của chính mình.
 *  - ADMIN_USER không được sửa role / thao tác trên SUPER_ADMIN.
 */
@Service
@RequiredArgsConstructor
public class AdminUpdateUserUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public AdminUserDetailOutput execute(AdminUpdateUserInput input) {
        User target = userRepository.findByIdForUpdate(input.targetUserId())
                .orElseThrow(() -> new UserNotFoundException(input.targetUserId()));

        boolean requesterIsSuperAdmin = "SUPER_ADMIN".equals(input.requesterRole());
        boolean targetIsSuperAdmin = target.getRole().isSuperAdmin();

        // ADMIN_USER không được thao tác trên SUPER_ADMIN
        if (targetIsSuperAdmin && !requesterIsSuperAdmin) {
            throw new IllegalStateException("Bạn không có quyền chỉnh sửa tài khoản SUPER_ADMIN.");
        }

        boolean isSelf = target.getId().equals(input.requesterId());

        // Đổi tên
        if (input.name() != null) {
            target.changeName(input.name());
        }

        // Đổi role
        if (input.roleName() != null && !input.roleName().isBlank()) {
            if (isSelf) {
                throw new IllegalStateException("Bạn không thể tự thay đổi vai trò của chính mình.");
            }
            Role newRole = roleRepository.findByName(input.roleName().toUpperCase().trim())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò: " + input.roleName()));
            target.changeRole(newRole);
        }

        // Đổi cờ nội bộ
        if (input.isInternal() != null) {
            target.setInternalFlag(input.isInternal());
        }

        // Đổi phòng ban
        if (input.departmentId() != null) {
            target.changeDepartment(input.departmentId());
        }

        User saved = userRepository.save(target);
        return AdminUserDetailOutput.from(saved);
    }
}
