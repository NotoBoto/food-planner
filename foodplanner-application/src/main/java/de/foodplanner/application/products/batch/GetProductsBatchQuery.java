package de.foodplanner.application.products.batch;

import java.util.List;

import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Eingabe des Sammelabrufs (§5.1); die Grenzen gelten hier, nie im Controller (§3.2).
 *
 * @param keys gespeicherte {@code productKey}s, höchstens {@value #MAX_KEYS}, jeder höchstens {@value #MAX_KEY_LENGTH}
 *             Zeichen. Leere, {@code null}- und falsch geformte Einträge sind keine Verletzung, sondern unbekannte
 *             Schlüssel; ein überlanger Eintrag ist dagegen kein veralteter Schlüssel, sondern eine fehlerhafte Anfrage
 */
@KeyLengthLimited
public record GetProductsBatchQuery(
        @NotNull(message = "darf nicht leer sein")
        @Size(max = GetProductsBatchQuery.MAX_KEYS, message = "darf höchstens 200 Schlüssel enthalten")
        List<@Nullable String> keys) {

    /** Höchstzahl der Schlüssel je Anfrage (§5.1). */
    public static final int MAX_KEYS = 200;

    /**
     * Höchstlänge eines Schlüssels. Artikelnummern und EANs sind weit kürzer; die Grenze hält Regex und SQL-Parameter
     * klein. Die Größe des Request-Bodys selbst begrenzt sie nicht, denn Jackson liest ihn vor der Validierung.
     */
    public static final int MAX_KEY_LENGTH = 64;
}
