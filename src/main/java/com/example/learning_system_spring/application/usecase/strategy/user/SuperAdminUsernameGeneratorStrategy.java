package com.example.learning_system_spring.application.usecase.strategy.user;

import org.springframework.stereotype.Component;

@Component
public class SuperAdminUsernameGeneratorStrategy extends AbstractUsernameGeneratorStrategy {
    @Override
    public boolean supports(String roleName) {
        return "SUPER_ADMIN".equalsIgnoreCase(roleName);
    }

    @Override
    protected String getPrefix() {
        return "SA";
    }
}
