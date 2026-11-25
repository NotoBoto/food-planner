package de.foodplanner.domain.shoppinglist;

import java.util.List;

/**
 * Gesamtsummen einer Einkaufsliste (§5.2). Geld ganzzahlig in Cent, Nährwerte ungerundet: Gerundet wird einmal an der
 * API-Grenze, nie pro Zeile vor dem Summieren (§4.4).
 *
 * @param priceCents           Summe der {@link ShoppingLine#lineTotalCents()}
 * @param nutrition            Summe der Nährwerte aller Zeilen, die welche haben; Zeilen ohne Nährwerte zählen nicht
 *                             mit, eine Liste ganz ohne ergibt {@link Nutrition#ZERO}
 * @param priceDifferenceCents Summe der {@code priceChangeCents × quantity} über alle Zeilen (§5.2)
 */
public record ShoppingTotals(long priceCents, Nutrition nutrition, long priceDifferenceCents) {

    /** Summiert die Zeilen; Geld mit {@link Math#addExact}, damit ein Überlauf nie still bleibt. */
    public static ShoppingTotals of(List<ShoppingLine> lines) {
        long priceCents = 0;
        long priceDifferenceCents = 0;
        Nutrition nutrition = Nutrition.ZERO;
        for (ShoppingLine line : lines) {
            priceCents = Math.addExact(priceCents, line.lineTotalCents());
            priceDifferenceCents = Math.addExact(priceDifferenceCents, line.priceDifferenceCents());
            Nutrition lineNutrition = line.nutrition();
            if (lineNutrition != null) {
                nutrition = nutrition.plus(lineNutrition);
            }
        }
        return new ShoppingTotals(priceCents, nutrition, priceDifferenceCents);
    }
}
