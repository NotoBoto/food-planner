package de.foodplanner.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import de.foodplanner.infrastructure.HttpProperties;
import de.foodplanner.infrastructure.ScrapeProperties;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

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
        assertThat(validator.validate(new HttpProperties("FoodPlanner (+mailto:ops@foodplanner.invalid)")))
                .isEmpty();
    }

    private static ScrapeProperties properties(int maxProducts) {
        return new ScrapeProperties(maxProducts, 4, 0.2, "http://localhost", "/sitemap_products.xml");
    }
}
