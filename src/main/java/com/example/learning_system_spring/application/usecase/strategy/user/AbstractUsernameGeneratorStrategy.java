package com.example.learning_system_spring.application.usecase.strategy.user;

import java.util.UUID;

public abstract class AbstractUsernameGeneratorStrategy implements UsernameGeneratorStrategy {
    
    protected abstract String getPrefix();

    @Override
    public String generateUsername() {
        return getPrefix() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
