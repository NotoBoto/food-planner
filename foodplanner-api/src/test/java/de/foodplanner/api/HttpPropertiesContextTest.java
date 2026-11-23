package de.foodplanner.api;

import static org.assertj.core.api.Assertions.assertThat;

import de.foodplanner.infrastructure.HttpProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class HttpPropertiesContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(HttpPropertiesConfiguration.class);

    @Test
    void apiContextRejectsMissingUserAgent() {
        contextRunner
                .withPropertyValues("http.user-agent=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void apiContextRejectsPlaceholderUserAgent() {
        contextRunner
                .withPropertyValues("http.user-agent=FoodPlanner (+mailto:foodplanner@example.org)")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void apiContextAcceptsContactAddress() {
        contextRunner
                .withPropertyValues("http.user-agent=FoodPlanner (+mailto:ops@foodplanner.dev)")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(HttpProperties.class)
    static class HttpPropertiesConfiguration {
    }
}
