package com.example.learning_system_spring.application.usecase.strategy.user;

import org.springframework.stereotype.Component;

@Component
public class StaffUsernameGeneratorStrategy extends AbstractUsernameGeneratorStrategy {
    @Override
    public boolean supports(String roleName) {
        return "STAFF".equalsIgnoreCase(roleName);
    }

    @Override
    protected String getPrefix() {
        return "STAFF";
    }
}
