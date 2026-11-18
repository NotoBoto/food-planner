package de.foodplanner.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ScrapedRowValidatorTest {

    private final ScrapedRowValidator validator = new ScrapedRowValidator(Set.of("Haushalt", "Garten"));

    private static Per100g plausible() {
        return new Per100g(new BigDecimal("250"), new BigDecimal("8"), new BigDecimal("10"), new BigDecimal("30"));
    }

    private static RawProduct raw(String name, Long price, String url, List<String> categories, Per100g nutrition) {
        return new RawProduct(name, price, url, categories, "https://img/x.jpg", "000123", nutrition);
    }

    @Test
    void acceptsValidRow() {
        var row = validator.validate(raw("Butter", 199L, "https://aldi/p/1", List.of("Molkerei", "Butter"), plausible()));

        assertThat(row).isPresent();
        assertThat(row.get().category()).isEqualTo("Butter");
        assertThat(row.get().notFood()).isFalse();
        assertThat(row.get().nutrition()).isNotNull();
    }

    @Test
    void enforcesPriceBounds() {
        assertThat(validator.validate(raw("A", 0L, "u", List.of(), null))).isEmpty();
        assertThat(validator.validate(raw("A", 100_001L, "u", List.of(), null))).isEmpty();
        assertThat(validator.validate(raw("A", null, "u", List.of(), null))).isEmpty();
        assertThat(validator.validate(raw("A", 1L, "u", List.of(), null))).isPresent();
        assertThat(validator.validate(raw("A", 100_000L, "u", List.of(), null))).isPresent();
    }

    @Test
    void enforcesNameBounds() {
        assertThat(validator.validate(raw("  ", 100L, "u", List.of(), null))).isEmpty();
        assertThat(validator.validate(raw(null, 100L, "u", List.of(), null))).isEmpty();
        assertThat(validator.validate(raw("x".repeat(301), 100L, "u", List.of(), null))).isEmpty();
        assertThat(validator.validate(raw("x".repeat(300), 100L, "u", List.of(), null))).isPresent();
    }

    @Test
    void enforcesUrlBounds() {
        assertThat(validator.validate(raw("A", 100L, null, List.of(), null))).isEmpty();
        assertThat(validator.validate(raw("A", 100L, "u".repeat(1001), List.of(), null))).isEmpty();
        assertThat(validator.validate(raw("A", 100L, "u".repeat(1000), List.of(), null))).isPresent();
    }

    @Test
    void marksNonFoodCategoryCaseInsensitively() {
        var row = validator.validate(raw("Spaten", 999L, "u", List.of("garten", "Werkzeug"), null)).orElseThrow();
        assertThat(row.notFood()).isTrue();
    }

    @Test
    void dropsImplausibleNutritionButKeepsRow() {
        var bad = new Per100g(new BigDecimal("1000"), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        var row = validator.validate(raw("A", 100L, "u", List.of(), bad)).orElseThrow();
        assertThat(row.nutrition()).isNull();
    }
}
