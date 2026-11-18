package de.foodplanner.application.shoppinglist.summarise;

import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

import de.foodplanner.application.products.batch.GetProductsBatchQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Eingabe der Listenzusammenfassung (§5.2): die Liste aus dem Browser und optionale Ziele. Die Grenzen gelten hier,
 * nie im Controller (§3.2), und entsprechen {@code web/src/limits.ts} (§7.2). Zahlen sind Wrapper-Typen, damit ein
 * fehlendes Feld als {@code null} an {@code @NotNull} scheitert, statt still {@code 0} zu werden, und durchweg
 * {@code Long}: Ein Wert jenseits von {@code int} wird so als Verletzung gemeldet, nicht als Lesefehler des JSON.
 *
 * @param items Zeilen der Liste, höchstens {@value #MAX_ITEMS}, jede vollständig; kein {@code null}-Eintrag und jeder
 *              {@code productKey} höchstens einmal (§7.2, {@link ItemsWellFormed})
 * @param goals Ziele, oder {@code null}, wenn keine gesetzt sind
 */
@ItemsWellFormed
public record SummariseShoppingListQuery(
        @NotNull(message = "darf nicht leer sein")
        @Size(max = SummariseShoppingListQuery.MAX_ITEMS, message = "darf höchstens 200 Einträge enthalten")
        @Valid
        @Nullable List<@Nullable Item> items,
        @Valid @Nullable Goals goals) {

    /** Höchstzahl der Zeilen je Anfrage (§5.2). */
    public static final int MAX_ITEMS = 200;

    /**
     * Eine Zeile der Liste (§4.6). Alle Felder sind Pflicht.
     *
     * @param productKey      gespeicherter Schlüssel, höchstens {@value GetProductsBatchQuery#MAX_KEY_LENGTH} Zeichen
     *                        wie im Sammelabruf. Ein falsch geformter Schlüssel ist kein Fehler, sondern ein Produkt,
     *                        das es nicht mehr gibt
     * @param quantity        ganze Packungen, 1…1000 (§4.4)
     * @param priceAtAddCents Preis beim Hinzufügen in Cent, 1…100 000
     * @param addedAt         Zeitpunkt des Hinzufügens; derselbe Zeitpunkt kommt zurück, ausgegeben in UTC
     */
    public record Item(
            @NotNull(message = "darf nicht leer sein")
            @Size(max = GetProductsBatchQuery.MAX_KEY_LENGTH, message = "darf höchstens 64 Zeichen lang sein")
            @Nullable String productKey,
            @NotNull(message = "darf nicht leer sein")
            @Min(value = 1, message = "muss zwischen 1 und 1000 liegen")
            @Max(value = 1000, message = "muss zwischen 1 und 1000 liegen")
            @Nullable Long quantity,
            @NotNull(message = "darf nicht leer sein")
            @Min(value = 1, message = "muss zwischen 1 und 100000 liegen")
            @Max(value = 100_000, message = "muss zwischen 1 und 100000 liegen")
            @Nullable Long priceAtAddCents,
            @NotNull(message = "darf nicht leer sein") @Nullable Instant addedAt) {
    }

    /**
     * Ziele der Liste (§7.2). Jedes ist einzeln optional; ein gesetztes Ziel ist eine ganze Zahl von 1 bis
     * {@link Integer#MAX_VALUE}. {@code Long} statt {@code Integer}, damit ein zu großer Wert hier gemeldet wird und
     * nicht schon beim Lesen des JSON scheitert. 0 ist kein Ziel, sondern ein Fehler: „kein Ziel" ist {@code null}.
     */
    public record Goals(
            @Min(value = 1, message = GOAL_RANGE) @Max(value = Integer.MAX_VALUE, message = GOAL_RANGE)
            @Nullable Long protein,
            @Min(value = 1, message = GOAL_RANGE) @Max(value = Integer.MAX_VALUE, message = GOAL_RANGE)
            @Nullable Long fat,
            @Min(value = 1, message = GOAL_RANGE) @Max(value = Integer.MAX_VALUE, message = GOAL_RANGE)
            @Nullable Long carbs,
            @Min(value = 1, message = GOAL_RANGE) @Max(value = Integer.MAX_VALUE, message = GOAL_RANGE)
            @Nullable Long kcal,
            @Min(value = 1, message = GOAL_RANGE) @Max(value = Integer.MAX_VALUE, message = GOAL_RANGE)
            @Nullable Long budgetCents) {

        private static final String GOAL_RANGE = "muss zwischen 1 und 2147483647 liegen";
    }
}
