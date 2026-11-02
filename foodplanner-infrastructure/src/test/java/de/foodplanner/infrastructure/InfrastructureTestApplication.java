package de.foodplanner.infrastructure;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/** Minimaler Spring-Kontext für Tests dieses Moduls, das selbst keine Anwendung startet. */
@SpringBootConfiguration
@EnableAutoConfiguration
public class InfrastructureTestApplication {
}
