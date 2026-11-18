package de.foodplanner.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Composition Root #1 (§3.1): bindet die Handler aus {@code application} und die Adapter aus {@code infrastructure}
 * ein, deshalb scannt sie deren Pakete mit.
 */
@SpringBootApplication(scanBasePackages = {
        "de.foodplanner.api", "de.foodplanner.application", "de.foodplanner.infrastructure"})
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
