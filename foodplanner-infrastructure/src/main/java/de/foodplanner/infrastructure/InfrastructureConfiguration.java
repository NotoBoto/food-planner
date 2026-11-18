package de.foodplanner.infrastructure;

import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
<<<<<<< HEAD
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
=======
>>>>>>> 6fec8dab926e0202e2ddbdf21a29b9a0bba6fa73
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
<<<<<<< HEAD
@ConditionalOnProperty(name = "ingestion.enabled", havingValue = "true")
=======
>>>>>>> 6fec8dab926e0202e2ddbdf21a29b9a0bba6fa73
@EnableConfigurationProperties({HttpProperties.class, ScrapeProperties.class})
public class InfrastructureConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    Sleeper sleeper() {
        return Sleeper.system();
    }

    @Bean
    RequestGate requestGate(Clock clock, Sleeper sleeper, ScrapeProperties properties) {
        return new RequestGate(clock, sleeper, Duration.ofMillis((long) (properties.scrapeDelaySeconds() * 1000)));
    }
}
