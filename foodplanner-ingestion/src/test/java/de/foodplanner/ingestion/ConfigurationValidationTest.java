package de.foodplanner.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import de.foodplanner.infrastructure.HttpProperties;
<<<<<<< HEAD
import de.foodplanner.infrastructure.InfrastructureConfiguration;
=======
>>>>>>> 6fec8dab926e0202e2ddbdf21a29b9a0bba6fa73
import de.foodplanner.infrastructure.ScrapeProperties;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
<<<<<<< HEAD
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
=======
>>>>>>> 6fec8dab926e0202e2ddbdf21a29b9a0bba6fa73

class ConfigurationValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsMaxProductsOutsideConfiguredRange() {
        assertThat(validator.validate(properties(0))).isNotEmpty();
        assertThat(validator.validate(properties(10_001))).isNotEmpty();
        assertThat(validator.validate(properties(100))).isEmpty();
    }

    @Test
    void rejectsMissingAndPlaceholderUserAgents() {
        assertThat(validator.validate(new HttpProperties(""))).isNotEmpty();
        assertThat(validator.validate(new HttpProperties("FoodPlanner (+mailto:foodplanner@example.org)")))
                .isNotEmpty();
<<<<<<< HEAD
        assertThat(validator.validate(new HttpProperties("FoodPlanner (+mailto:ops@foodplanner.dev)")))
                .isEmpty();
    }

    @Test
    void applicationContextRejectsInvalidMaxProductsAtStartup() {
        new ApplicationContextRunner()
                .withUserConfiguration(InfrastructureConfiguration.class)
                .withPropertyValues(
                        "ingestion.enabled=true",
                        "http.user-agent=FoodPlanner (mailto:ops@foodplanner.dev)",
                        "ingestion.aldi-max-products=0",
                        "ingestion.scrape-concurrency=4",
                        "ingestion.scrape-delay-seconds=0",
                        "ingestion.aldi-base-url=http://localhost",
                        "ingestion.aldi-sitemap-path=/sitemap_products.xml")
                .run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }

=======
        assertThat(validator.validate(new HttpProperties("FoodPlanner (+mailto:ops@foodplanner.invalid)")))
                .isEmpty();
    }

>>>>>>> 6fec8dab926e0202e2ddbdf21a29b9a0bba6fa73
    private static ScrapeProperties properties(int maxProducts) {
        return new ScrapeProperties(maxProducts, 4, 0.2, "http://localhost", "/sitemap_products.xml");
    }
}
