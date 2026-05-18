package com.example.learning_system_spring.application.usecase.strategy.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UsernameGeneratorFactory {

    private final List<UsernameGeneratorStrategy> strategies;

    public String generateUsername(String roleName) {
        return strategies.stream()
            .filter(strategy -> !(strategy instanceof DefaultUsernameGeneratorStrategy))
            .filter(strategy -> strategy.supports(roleName))
            .findFirst()
            .orElseGet(() -> strategies.stream()
                .filter(strategy -> strategy instanceof DefaultUsernameGeneratorStrategy)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No default strategy found")))
            .generateUsername();
    }
}
