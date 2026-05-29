package com.example.learning_system_spring.adapter.dto.request.User;

import com.example.learning_system_spring.application.dto.User.ChangePasswordInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChangePasswordRequest {

    @NotBlank
    private String currentPassword;

    @NotBlank
    @Size(min = 6, max = 100)
    private String newPassword;

    public ChangePasswordInput toInput(Long userId) {
        return new ChangePasswordInput(userId, currentPassword, newPassword);
    }
}
