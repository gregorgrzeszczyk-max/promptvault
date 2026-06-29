package com.promptvault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the PromptVault Spring Boot application.
 *
 * Sample data (the predefined admin account, demo users, categories, policy
 * keywords and prompts) is created on first startup by
 * {@link com.promptvault.component.DataSeeder}.
 */
@SpringBootApplication
public class PromptvaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromptvaultApplication.class, args);
    }
}
