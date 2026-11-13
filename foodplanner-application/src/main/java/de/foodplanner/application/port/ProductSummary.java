package de.foodplanner.application.port;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/**
 * Read-Model eines Suchtreffers (§5.1). Geld in Cent, Nährwerte pro 100 g auf eine Nachkommastelle gerundet.
 *
 * @param pricePerKgCents Preis je Kilogramm in Cent, {@code HALF_UP} gerundet
 */
public record ProductSummary(
        long id,
        String productKey,
        String name,
        @Nullable String brand,
        @Nullable String category,
        int packageGrams,
        String unit,
        long priceCents,
        long pricePerKgCents,
        @Nullable BigDecimal protein100g,
        @Nullable BigDecimal fat100g,
        @Nullable BigDecimal carbs100g,
        @Nullable BigDecimal kcal100g) {
}
