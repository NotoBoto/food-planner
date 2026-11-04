package de.foodplanner.domain.product;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PricePerKgTest {

    @Test
    void exactDivision() {
        assertThat(PricePerKg.cents(199, 500)).isEqualTo(398);
        assertThat(PricePerKg.cents(129, 1000)).isEqualTo(129);
    }

    @Test
    void roundsHalfUp() {
        assertThat(PricePerKg.cents(129, 750)).isEqualTo(172);
        assertThat(PricePerKg.cents(100, 3)).isEqualTo(33333);
        assertThat(PricePerKg.cents(5, 3)).isEqualTo(1667);
        assertThat(PricePerKg.cents(1, 2000)).isEqualTo(1);
    }
}
