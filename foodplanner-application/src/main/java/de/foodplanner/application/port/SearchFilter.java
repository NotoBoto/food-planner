package de.foodplanner.application.port;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/**
 * Filter der Produktsuche (§5.1); {@code null} bedeutet „nicht gesetzt".
 *
 * @param category       exakte Kategorie
 * @param minPriceCents  Mindestpreis in Cent
 * @param maxPriceCents  Höchstpreis in Cent
 * @param minProtein100g Mindestprotein pro 100 g
 */
public record SearchFilter(
        @Nullable String category,
        @Nullable Long minPriceCents,
        @Nullable Long maxPriceCents,
        @Nullable BigDecimal minProtein100g) {

    /** Filter ohne jede Einschränkung. */
    public static SearchFilter none() {
        return new SearchFilter(null, null, null, null);
    }
}
