package de.foodplanner.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HttpPropertiesTest {

    @Test
    void acceptsARealContactAddress() {
        HttpProperties properties = new HttpProperties("FoodPlanner (mailto:ops@foodplanner.example)");

        assertThat(properties.isContactAddressValid()).isTrue();
    }

    @Test
    void rejectsPlaceholderAddresses() {
        HttpProperties properties = new HttpProperties("FoodPlanner (mailto:foodplanner@example.org)");

        assertThat(properties.isContactAddressValid()).isFalse();
    }
}
