package de.foodplanner.infrastructure;

import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "ingestion.enabled", havingValue = "true")
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
