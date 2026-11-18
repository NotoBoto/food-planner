package de.foodplanner.application.shoppinglist.summarise;

import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

import de.foodplanner.domain.shoppinglist.GoalProgress;
import de.foodplanner.domain.shoppinglist.Nutrition;
import de.foodplanner.domain.shoppinglist.ShoppingLine;
import de.foodplanner.domain.shoppinglist.ShoppingTotals;

/**
 * Ergebnis der Listenzusammenfassung (§5.2), noch ungerundet: Die API rundet die Nährwerte an genau einer Stelle auf
 * eine Nachkommastelle (§4.4).
 *
 * @param lines              die Zeilen, deren Produkt im Katalog ist, in der Reihenfolge der Anfrage
 * @param totals             Summen über {@code lines}
 * @param goalProgress       Fortschritt zu den Zielen, {@code null} ohne Ziele
 * @param missingProductKeys Schlüssel, wie gesendet, deren Produkt unbekannt ist, keinen aktuellen Preis hat oder die
 *                           falsch geformt sind; in der Reihenfolge der Anfrage
 */
public record ShoppingListSummary(
        List<Line> lines,
        ShoppingTotals totals,
        @Nullable GoalProgress goalProgress,
        List<String> missingProductKeys) {

    /**
     * Eine Zeile mit den Angaben, die nur zur Anzeige dienen, und der Rechnung aus {@code domain}.
     *
     * @param addedAt Zeitpunkt des Hinzufügens, unverändert aus der Anfrage
     * @param line    Menge, Preise, Packungsgröße und Nährwerte pro 100 g
     * @param per100g Nährwerte pro 100 g bzw. 100 ml, {@code null}, wenn das Produkt keine hat
     */
    public record Line(
            String productKey,
            String name,
            String unit,
            Instant addedAt,
            ShoppingLine line,
            @Nullable Nutrition per100g) {
    }
}
