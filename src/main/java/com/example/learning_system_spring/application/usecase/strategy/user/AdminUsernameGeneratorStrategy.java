package com.example.learning_system_spring.application.usecase.strategy.user;

import org.springframework.stereotype.Component;

@Component
public class AdminUsernameGeneratorStrategy extends AbstractUsernameGeneratorStrategy {
    @Override
    public boolean supports(String roleName) {
        return "ADMIN_USER".equalsIgnoreCase(roleName);
    }

    @Override
    protected String getPrefix() {
        return "AD";
    }
}
