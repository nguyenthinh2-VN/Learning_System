package com.example.learning_system_spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LearningSystemSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearningSystemSpringApplication.class, args);
    }

}
