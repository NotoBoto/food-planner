package de.foodplanner.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProductKeyTest {

    @Test
    void aldiSkuWins() {
        assertThat(ProductKey.of("123", "4008").value()).isEqualTo("aldi:123");
    }

    @Test
    void offCodeWhenNoSku() {
        assertThat(ProductKey.of(null, "4008").value()).isEqualTo("off:4008");
    }

    @Test
    void neitherIsRejected() {
        assertThatThrownBy(() -> ProductKey.of(null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toStringIsValue() {
        assertThat(ProductKey.of("123", null)).hasToString("aldi:123");
    }
}
