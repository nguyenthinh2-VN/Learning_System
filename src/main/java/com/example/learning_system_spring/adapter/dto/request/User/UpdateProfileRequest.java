package com.example.learning_system_spring.adapter.dto.request.User;

import com.example.learning_system_spring.application.dto.User.UpdateMyProfileInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateProfileRequest {

    @NotBlank
    @Size(min = 1, max = 200)
    private String name;

    @Size(max = 500)
    private String avatarUrl; // tùy chọn: null = giữ nguyên, "" = xóa avatar

    public UpdateMyProfileInput toInput(Long userId) {
        return new UpdateMyProfileInput(userId, name, avatarUrl);
    }
}
