package de.foodplanner.domain.shoppinglist;

/**
 * Ziele der Einkaufsliste (§7.2). Jedes Ziel ist einzeln optional: {@code null} heißt „nicht gesetzt". Ein gesetztes
 * Ziel ist mindestens 1; das prüft die Validierung der Anfrage, bevor gerechnet wird.
 *
 * @param protein     Eiweiß in Gramm
 * @param fat         Fett in Gramm
 * @param carbs       Kohlenhydrate in Gramm
 * @param kcal        Energie in Kilokalorien
 * @param budgetCents Budget in Cent
 */
public record Goals(Long protein, Long fat, Long carbs, Long kcal, Long budgetCents) {
}
