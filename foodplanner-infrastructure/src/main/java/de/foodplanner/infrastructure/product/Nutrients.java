package de.foodplanner.infrastructure.product;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.jspecify.annotations.Nullable;

/**
 * Nährwerte bleiben {@code BigDecimal} und werden erst an der API-Grenze auf eine Nachkommastelle gerundet (§4.4).
 * Suche und Detail runden über diese eine Stelle gleich.
 */
final class Nutrients {

    private Nutrients() {
    }

    /** Rundet {@code HALF_UP} auf eine Nachkommastelle; {@code NULL} bleibt {@code null}. */
    static @Nullable BigDecimal round(@Nullable BigDecimal value) {
        return value == null ? null : value.setScale(1, RoundingMode.HALF_UP);
    }
}
