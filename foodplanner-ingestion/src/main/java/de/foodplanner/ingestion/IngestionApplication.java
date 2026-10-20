package de.foodplanner.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IngestionApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(IngestionApplication.class, args)));
    }
}
