package de.foodplanner.domain.shoppinglist;

import java.math.BigDecimal;

/**
 * Die vier Makronährstoffe, ungerundet. Nährwerte bleiben {@code BigDecimal} und werden erst an der API-Grenze auf
 * eine Nachkommastelle gerundet (§4.4). Je nach Zusammenhang pro 100 g (bzw. 100 ml) oder absolut für eine Zeile.
 *
 * @param protein Eiweiß in Gramm
 * @param fat     Fett in Gramm
 * @param carbs   Kohlenhydrate in Gramm
 * @param kcal    Energie in Kilokalorien
 */
public record Nutrition(BigDecimal protein, BigDecimal fat, BigDecimal carbs, BigDecimal kcal) {

    /** Neutrales Element der Addition: die Summe einer Liste ohne Nährwerte. */
    public static final Nutrition ZERO =
            new Nutrition(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    /** Komponentenweise Summe, exakt. */
    public Nutrition plus(Nutrition other) {
        return new Nutrition(protein.add(other.protein), fat.add(other.fat), carbs.add(other.carbs),
                kcal.add(other.kcal));
    }

    /** Komponentenweise mit {@code factor} multipliziert und durch 100 geteilt, exakt (Dezimalkomma verschoben). */
    Nutrition timesOver100(long factor) {
        BigDecimal multiplier = BigDecimal.valueOf(factor);
        return new Nutrition(
                protein.multiply(multiplier).movePointLeft(2),
                fat.multiply(multiplier).movePointLeft(2),
                carbs.multiply(multiplier).movePointLeft(2),
                kcal.multiply(multiplier).movePointLeft(2));
    }
}
