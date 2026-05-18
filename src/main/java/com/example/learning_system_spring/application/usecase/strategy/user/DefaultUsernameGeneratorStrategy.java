package com.example.learning_system_spring.application.usecase.strategy.user;

import org.springframework.stereotype.Component;

@Component
public class DefaultUsernameGeneratorStrategy extends AbstractUsernameGeneratorStrategy {
    @Override
    public boolean supports(String roleName) {
        return true; // Fallback for any other role
    }

    @Override
    protected String getPrefix() {
        return "USR";
    }
}
