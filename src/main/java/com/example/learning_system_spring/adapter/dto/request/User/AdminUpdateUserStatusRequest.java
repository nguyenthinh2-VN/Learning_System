package com.example.learning_system_spring.adapter.dto.request.User;

import com.example.learning_system_spring.application.dto.User.AdminUpdateUserStatusInput;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminUpdateUserStatusRequest {

    @NotNull
    private Boolean enabled;

    public AdminUpdateUserStatusInput toInput(Long targetUserId, Long requesterId, String requesterRole) {
        return new AdminUpdateUserStatusInput(targetUserId, requesterId, requesterRole, enabled);
    }
}
