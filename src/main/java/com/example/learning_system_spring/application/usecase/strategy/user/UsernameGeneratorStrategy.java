package com.example.learning_system_spring.application.usecase.strategy.user;

public interface UsernameGeneratorStrategy {
    boolean supports(String roleName);
    String generateUsername();
}
