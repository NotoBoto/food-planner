package de.foodplanner.domain.shoppinglist;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class ShoppingTotalsTest {

    private static Nutrition per100g(String protein, String fat, String carbs, String kcal) {
        return new Nutrition(new BigDecimal(protein), new BigDecimal(fat), new BigDecimal(carbs), new BigDecimal(kcal));
    }

    @Test
    void sumsLines() {
        ShoppingLine dearer = new ShoppingLine(3, 139, 129, 1000, per100g("3.4", "1.5", "4.8", "46"));
        ShoppingLine cheaper = new ShoppingLine(1, 164, 199, 500, per100g("8", "1", "45", "230"));

        ShoppingTotals totals = ShoppingTotals.of(List.of(dearer, cheaper));

        assertThat(totals.priceCents()).isEqualTo(3 * 139 + 164);
        assertThat(totals.priceDifferenceCents()).isEqualTo(30 - 35);
        assertThat(totals.nutrition().protein()).isEqualByComparingTo("142");
        assertThat(totals.nutrition().fat()).isEqualByComparingTo("50");
        assertThat(totals.nutrition().carbs()).isEqualByComparingTo("369");
        assertThat(totals.nutrition().kcal()).isEqualByComparingTo("2530");
    }

    @Test
    void emptyListIsZero() {
        ShoppingTotals totals = ShoppingTotals.of(List.of());

        assertThat(totals.priceCents()).isZero();
        assertThat(totals.priceDifferenceCents()).isZero();
        assertThat(totals.nutrition()).isEqualTo(Nutrition.ZERO);
    }

    @Test
    void linesWithoutNutritionAreSkipped() {
        ShoppingLine without = new ShoppingLine(2, 189, 189, 750, null);
        ShoppingLine with = new ShoppingLine(1, 139, 139, 1000, per100g("3.4", "1.5", "4.8", "46"));

        ShoppingTotals totals = ShoppingTotals.of(List.of(without, with));

        assertThat(totals.priceCents()).isEqualTo(2 * 189 + 139);
        assertThat(totals.nutrition().protein()).isEqualByComparingTo("34");
        assertThat(totals.nutrition().kcal()).isEqualByComparingTo("460");
    }

    @Test
    void totalsRoundOnlyOnce() {
        // Jede Zeile 0,04 g Eiweiß, gerundet 0,0; die Summe 0,12 ergibt erst gerundet 0,1.
        ShoppingLine line = new ShoppingLine(1, 99, 99, 100, per100g("0.04", "0", "0", "0"));

        ShoppingTotals totals = ShoppingTotals.of(List.of(line, line, line));

        assertThat(totals.nutrition().protein()).isEqualByComparingTo("0.12");
    }
}
