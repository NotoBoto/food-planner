package de.foodplanner.application.products.batch;

import java.util.List;

import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Eingabe des Sammelabrufs (§5.1); die Grenze gilt hier, nie im Controller (§3.2).
 *
 * @param keys gespeicherte {@code productKey}s, höchstens {@value #MAX_KEYS}. Leere, {@code null}- und falsch geformte
 *             Einträge sind keine Verletzung, sondern unbekannte Schlüssel
 */
public record GetProductsBatchQuery(
        @NotNull(message = "darf nicht leer sein")
        @Size(max = GetProductsBatchQuery.MAX_KEYS, message = "darf höchstens 200 Schlüssel enthalten")
        List<@Nullable String> keys) {

    /** Höchstzahl der Schlüssel je Anfrage (§5.1). */
    public static final int MAX_KEYS = 200;
}
