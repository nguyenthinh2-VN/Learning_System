package com.example.learning_system_spring.adapter.dto.request;

import com.example.learning_system_spring.application.dto.CreateUserInput;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateUserRequest {
    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 6, max = 100)
    private String password;

    @NotBlank @Size(min = 1, max = 200)
    private String name;

    @NotBlank
    private String roleName;

    @NotNull
    private Boolean isInternal;

    public CreateUserInput toInput() {
        return new CreateUserInput(email, password, name, roleName, isInternal);
    }
}
