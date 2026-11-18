package de.foodplanner.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HttpPropertiesTest {

    @Test
    void acceptsARealContactAddress() {
<<<<<<< HEAD
        HttpProperties properties = new HttpProperties("FoodPlanner (mailto:ops@foodplanner.dev)");
=======
        HttpProperties properties = new HttpProperties("FoodPlanner (mailto:ops@foodplanner.example)");
>>>>>>> 6fec8dab926e0202e2ddbdf21a29b9a0bba6fa73

        assertThat(properties.isContactAddressValid()).isTrue();
    }

    @Test
    void rejectsPlaceholderAddresses() {
        HttpProperties properties = new HttpProperties("FoodPlanner (mailto:foodplanner@example.org)");

        assertThat(properties.isContactAddressValid()).isFalse();
    }
<<<<<<< HEAD

    @Test
    void rejectsReservedSubdomainsAndTlds() {
        assertThat(new HttpProperties("mailto:ops@mail.example.org").isContactAddressValid()).isFalse();
        assertThat(new HttpProperties("mailto:ops@foodplanner.invalid").isContactAddressValid()).isFalse();
        assertThat(new HttpProperties("mailto:ops@foodplanner.test").isContactAddressValid()).isFalse();
    }
=======
>>>>>>> 6fec8dab926e0202e2ddbdf21a29b9a0bba6fa73
}
