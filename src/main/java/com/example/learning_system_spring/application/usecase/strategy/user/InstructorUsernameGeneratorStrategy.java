package com.example.learning_system_spring.application.usecase.strategy.user;

import org.springframework.stereotype.Component;

@Component
public class InstructorUsernameGeneratorStrategy extends AbstractUsernameGeneratorStrategy {
    @Override
    public boolean supports(String roleName) {
        return "INSTRUCTOR".equalsIgnoreCase(roleName);
    }

    @Override
    protected String getPrefix() {
        return "GV";
    }
}
