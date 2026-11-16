package de.foodplanner.application.products.search;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import de.foodplanner.application.port.SearchSort;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Eingabe der Produktsuche (§5.1); die Grenzen gelten hier, nie im Controller (§3.2).
 *
 * @param q              Suchtext oder EAN; {@code null} oder leer führt zur Browse-Suche. Höchstens
 *                       {@value #MAX_Q_LENGTH} Zeichen: jedes Token vergrößert die SQL-Abfrage und ihre Kosten je Zeile
 * @param category       exakte Kategorie
 * @param minPriceCents  Mindestpreis in Cent
 * @param maxPriceCents  Höchstpreis in Cent
 * @param minProtein100g Mindestprotein pro 100 g
 * @param sort           Sortierung
 * @param page           Seitennummer, ab 1
 * @param pageSize       Einträge pro Seite
 */
@PriceRangeOrdered
public record SearchProductsQuery(
        @Size(max = SearchProductsQuery.MAX_Q_LENGTH, message = "darf höchstens 200 Zeichen lang sein")
        @Nullable String q,
        @Nullable String category,
        @Nullable Long minPriceCents,
        @Nullable Long maxPriceCents,
        @Nullable BigDecimal minProtein100g,
        @NotNull(message = "darf nicht leer sein") SearchSort sort,
        @Min(value = 1, message = "muss zwischen 1 und 100000 liegen")
        @Max(value = 100000, message = "muss zwischen 1 und 100000 liegen") int page,
        @Min(value = 1, message = "muss zwischen 1 und 100 liegen")
        @Max(value = 100, message = "muss zwischen 1 und 100 liegen") int pageSize) {

    /** Höchstlänge von {@code q}; begrenzt Zahl der Tokens und damit Größe und Kosten der lexikalischen Abfrage. */
    public static final int MAX_Q_LENGTH = 200;
}
