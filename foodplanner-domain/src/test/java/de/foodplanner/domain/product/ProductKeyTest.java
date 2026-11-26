package de.foodplanner.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    @Test
    void parsesAldiKey() {
        ProductKey key = ProductKey.parse("aldi:1234567").orElseThrow();

        assertThat(key.value()).isEqualTo("aldi:1234567");
        assertThat(key.aldiSku()).contains("1234567");
        assertThat(key.offCode()).isEmpty();
    }

    @Test
    void parsesOffKey() {
        ProductKey key = ProductKey.parse("off:4008400401211").orElseThrow();

        assertThat(key.value()).isEqualTo("off:4008400401211");
        assertThat(key.offCode()).contains("4008400401211");
        assertThat(key.aldiSku()).isEmpty();
    }

    @Test
    void parseIsInverseOfOf() {
        assertThat(ProductKey.parse(ProductKey.of("123", "4008").value())).contains(ProductKey.of("123", "4008"));
        assertThat(ProductKey.parse(ProductKey.of(null, "4008").value())).contains(ProductKey.of(null, "4008"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "foo", "1234567", "aldi:", "off:", "aldi", "ALDI:123", "ean:4008", " aldi:123",
        "aldi:123 ", "aldi: 123", "aldi:12 34", "off:\t4008", ":123",
        "aldi:1001\u00A0", "aldi:\u20071001", "off:40\u300008"})
    void malformedKeyIsEmpty(String raw) {
        assertThat(ProductKey.parse(raw)).isEmpty();
    }

    @Test
    void nullIsEmpty() {
        assertThat(ProductKey.parse(null)).isEqualTo(Optional.empty());
    }
}
