package com.example.learning_system_spring.adapter.dto.request.User;

import com.example.learning_system_spring.application.dto.User.AdminUpdateUserInput;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tất cả field nullable — chỉ cập nhật field được gửi lên.
 */
@Getter
@Setter
@NoArgsConstructor
public class AdminUpdateUserRequest {

    @Size(min = 1, max = 200)
    private String name;

    private String roleName;

    private Boolean isInternal;

    private Long departmentId;

    public AdminUpdateUserInput toInput(Long targetUserId, Long requesterId, String requesterRole) {
        return new AdminUpdateUserInput(targetUserId, requesterId, requesterRole, name, roleName, isInternal, departmentId);
    }
}
