package de.foodplanner.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HttpPropertiesTest {

    @Test
    void acceptsARealContactAddress() {
        HttpProperties properties = new HttpProperties("FoodPlanner (mailto:ops@foodplanner.dev)");

        assertThat(properties.isContactAddressValid()).isTrue();
    }

    @Test
    void rejectsPlaceholderAddresses() {
        HttpProperties properties = new HttpProperties("FoodPlanner (mailto:foodplanner@example.org)");

        assertThat(properties.isContactAddressValid()).isFalse();
    }

    @Test
    void rejectsReservedSubdomainsAndTlds() {
        assertThat(new HttpProperties("mailto:ops@mail.example.org").isContactAddressValid()).isFalse();
        assertThat(new HttpProperties("mailto:ops@foodplanner.invalid").isContactAddressValid()).isFalse();
        assertThat(new HttpProperties("mailto:ops@foodplanner.test").isContactAddressValid()).isFalse();
    }
}
