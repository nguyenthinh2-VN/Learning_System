package com.example.learning_system_spring.adapter.dto.request;

import com.example.learning_system_spring.application.dto.LoginInput;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {
    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;

    public LoginInput toInput() {
        return new LoginInput(email, password);
    }
}
