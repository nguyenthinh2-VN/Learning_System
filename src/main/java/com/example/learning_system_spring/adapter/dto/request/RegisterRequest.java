package com.example.learning_system_spring.adapter.dto.request;

import com.example.learning_system_spring.application.dto.RegisterInput;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 6, max = 100)
    private String password;

    @NotBlank @Size(min = 1, max = 200)
    private String name;

    public RegisterInput toInput() {
        return new RegisterInput(email, password, name);
    }
}
